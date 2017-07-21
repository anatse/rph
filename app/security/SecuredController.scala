package security

import java.util.UUID
import javax.inject._

import com.google.inject.Injector
import com.google.inject.name.Names
import model.User
import play.api.{Configuration, Play}
import play.api.cache.{NamedCache, SyncCacheApi}
import play.api.inject.{BindingKey, QualifierInstance}
import play.api.libs.json.JsPath
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.cache.NamedCache

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class SessionContext (user: User)

/**
  * Created by asementsov on 23.03.17.
  */
abstract class SecuredController @Inject()(injector: Injector, components: ControllerComponents) extends AbstractController(components) {
  object AuthenticatedAction extends DefaultActionBuilder {
    override def executionContext: ExecutionContext = components.executionContext
    override def parser: BodyParser[AnyContent] = components.parsers.anyContent
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      println (request.session)

      val cache = injector.getInstance(classOf[CachedAuth])
      println ("cache: " + cache.getCache + ", " + cache.getCache.get[String]("test"))

      cache.getCache.set("test", "test")



      if (request.session.isEmpty) {
        Future {Results.Redirect("/login")}
      }
      else {
//        val qualifier = Some(QualifierInstance(NamedCache))
//        val bindingKey = BindingKey[SyncCacheApi](Class[SyncCacheApi], qualifier)
//        injector.getInstance(classOf[SyncCacheApi])

        request.session.get("username") match {
          case Some(user) => block(request)
          case None => Future {Results.Redirect("/login")}
        }
      }
    }
  }
}

class CachedAuth @Inject()(
    @NamedCache("auth-cache") authCache: SyncCacheApi,
    @NamedCache("session-cache") sessionCache: SyncCacheApi) {
  def setCache = {

  }

  def getCache = {
    authCache
  }
}

trait AuthProvider {
  def login(redirectUrl: String)
}

class GithubAuthProvider @Inject()(
    @NamedCache("auth-cache") authCache: SyncCacheApi,
    @NamedCache("session-cache") sessionCache: SyncCacheApi,
    configuration: Configuration,
    components: ControllerComponents,
    wsClient: WSClient) extends AbstractController(components) {
  private val REDIRECT_URI = "redirect_uri"

  private val STATE = "state"

  def githubLogin(redirectUrl: String) = Action {
    val clientUrl = configuration.get[String]("github.client_url")
    val clientId = configuration.get[String]("github.client_id")
    val authUrl = configuration.get[String]("github.auth_url")
    val stateTTL = configuration.get[Long]("github." + STATE + "_ttl")

    val scope = "user"
    val state = UUID.randomUUID().toString

    // Set temporary values for current state
    authCache.set(STATE, state, stateTTL seconds)
    authCache.set(REDIRECT_URI, clientUrl + "/" + redirectUrl)

    // redirect to github auth URL with state parameter
    val params = Map(
      "client_id" -> Seq(clientId),
      "redirect_uri" -> Seq(clientUrl + "/auth/github"),
      "scope" -> Seq(scope),
      "state" -> Seq(state)
    )

    Redirect("https://github.com/login/oauth/authorize", params, TEMPORARY_REDIRECT)
  }

  def readUserInfo (accessToken: String):Future[Map[String, String]] = {
    val userRq = wsClient.url(" https://api.github.com/user").withHeaders("Accept" -> "application/json").withQueryString("access_token" -> accessToken).get()
    userRq.map(resp => {
      val rs = resp.json

      val username = (rs \ "login").asOpt[String].getOrElse("")
      val email = (rs \ "email").asOpt[String].getOrElse("")

      println (rs)

      Map ("username" -> username, "email" -> email)
    })
  }

  def auth(code: String, state: String) = Action.async {
    val expectedState = authCache.get[String](STATE)
    if (state != expectedState.getOrElse("-")) {
      Future{BadRequest("wrong auth request: " + expectedState + ", " + state)}
    }
    else {
      val clientUrl = configuration.get[String]("github.client_url")
      val clientId = configuration.get[String]("github.client_id")
      val tokenUrl = configuration.get[String]("github.token_url")
      val authSecret = configuration.get[String]("github.auth_secret")
      val redirectUrl = authCache.get[String](REDIRECT_URI).getOrElse(clientUrl)

      val tokenRq = wsClient.url(tokenUrl).withHeaders("Accept" -> "application/json").withQueryString(
        "client_id" -> clientId,
        "redirect_uri" -> redirectUrl,
        "client_secret" -> "71d5204d9d9bbe9e9031a7a26e8a0102d06c20f3",
        "code" -> code,
        "state" -> state
      )

      val tokenRqQuery = tokenRq.post("")
      tokenRqQuery.map(resp => {
        val rs = resp
        if (rs.status == OK) {
          (rs.json \ "access_token").asOpt[String] match {
            case Some(accessToken) =>
              val ui = readUserInfo (accessToken)
              val username = Await.result(ui, 10 second).get("username").getOrElse("")
              println("username: " + username + ", " + redirectUrl)
              Redirect("/").withSession("username" -> username, "email" -> "test@mail.com")

            case _ => BadRequest ("Authentication error")
          }
        }
        else {
          BadRequest("what a fuck!")
        }
      })
    }
  }
}

@Singleton
class AuthRouter @Inject()(
      @NamedCache("auth-cache") authCache: SyncCacheApi,
      @NamedCache("session-cache") sessionCache: SyncCacheApi,
      configuration: Configuration,
      components: ControllerComponents,
      wsClient: WSClient) extends AbstractController(components) {
  val SessionKey = "SK"

  def login = Action { implicit request =>
    // Generate session identifier
    val sessionUuid = UUID.randomUUID().toString

    println (request.session.get("username"))


    Ok("hello eorld").withSession( ("username" -> "sessionUuid"))
  }

  def logout = Action {
    sessionCache.remove(SessionKey)
    Redirect("/").withNewSession
  }
}
