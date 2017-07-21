package controllers

import java.util.UUID
import javax.inject._

import play.api.cache.{CacheApi, NamedCache, SyncCacheApi}
import com.google.inject.Injector
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.mvc.Security._
import play.filters.csrf._
import security.SecuredController
import services.Counter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * This controller demonstrates how to use dependency injection to
 * bind a component into a controller class. The class creates an
 * `Action` that shows an incrementing count to users. The [[Counter]]
 * object is injected by the Guice dependency injection system.
  *
  * https://www.playframework.com/documentation/2.5.x/ScalaCache
 */
@Singleton
class CountController @Inject()(injector: Injector, components: ControllerComponents,
                                counter: Counter)
                               (addToken: CSRFAddToken, checkToken: CSRFCheck) extends SecuredController(injector, components) {

  // https://github.com/settings/applications/498629
  // https://deadbolt-scala.readme.io/v2.5/docs

//  def username(request: RequestHeader) = request.session.get("email")
//    def onUnauthorized(request: RequestHeader) = Results.Redirect("/login")
//
//    def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
//        Authenticated(username, onUnauthorized) { user =>
//            Action {
//              Ok("ok")
//            }
//          }
//      }
//
//   //then in a controller
//   def index = isAuthenticated { username => implicit request =>
//         Ok("Hello " + username)
//     }

//  def auth(code: String) = Action.async {
//    // Request accepted trying to post
//    val tokenRq = wsClient.url("https://github.com/login/oauth/access_token").withHeaders("Accept" -> "application/json").withQueryString(
//      "client_id" -> clientId,
//      "redirect_uri" -> "http://localhost:9000/count",
//      "client_secret" -> "71d5204d9d9bbe9e9031a7a26e8a0102d06c20f3",
//      "code" -> code,
//      "state" -> state
//    )
//
//    val tokenRqQuery = tokenRq.post("")
//    tokenRqQuery.map(resp => {
//      val rs = resp
//      if (rs.status == OK) {
//        println(rs.status + " " + rs.statusText + " " + (rs.json \ "access_token").as[String])
//        Ok(counter.nextCount().toString).withSession("githubToken" -> (rs.json \ "access_token").as[String])
//      }
//      else {
//        BadRequest("what a fuck!")
//      }
//    })
//  }

  /**
   * Create an action that responds with the [[Counter]]'s current
   * count. The result is plain text. This `Action` is mapped to
   * `GET /count` requests by an entry in the `routes` config file.
    *
    * AuthAcrtion
    * http://stackoverflow.com/questions/19868153/authorisation-check-in-controller-scala-play
   */
  def count = //checkToken {
    AuthenticatedAction {
      Ok(counter.nextCount().toString)
//      implicit request =>
//      if (request.session.isEmpty) {
//        println ("empty session")
//      }
//
//      request.session.get("githubToken") match {
//        case Some(token) =>
//          val userRq = wsClient.url(" https://api.github.com/user").withHeaders("Accept" -> "application/json").withQueryString("access_token" -> token).get()
//          userRq.map(resp => {
//            println(resp.json)
//            Ok(resp.json)
//          })
//
//        case _ =>
//          // https://github.com/login/oauth/authorize?client_id=a4ea8ede954b7655f7c7&redirect_url=http://localhost:9000/auth&scope=user&state=5678
//          Future {
//            Redirect(
//              "https://github.com/login/oauth/authorize",
//              Map("client_id" -> Seq(clientId),
//                "redirect_uri" -> Seq("http://localhost:9000/auth"),
//                "scope" -> Seq(scope),
//                "state" -> Seq(state)
//              )
//            )
//          }
//      }
    }
  //}

}
