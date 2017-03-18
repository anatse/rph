package controllers

import java.util.UUID
import javax.inject._

import play.api._
import play.api.libs.oauth.{ConsumerKey, OAuth, RequestToken, ServiceInfo}
import play.api.libs.openid.OpenIdClient
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.filters.csrf._
import services.Counter

/**
 * This controller demonstrates how to use dependency injection to
 * bind a component into a controller class. The class creates an
 * `Action` that shows an incrementing count to users. The [[Counter]]
 * object is injected by the Guice dependency injection system.
 */
@Singleton
class CountController @Inject()(components: ControllerComponents, counter: Counter)(addToken: CSRFAddToken, checkToken: CSRFCheck) extends AbstractController(components) {

  // https://github.com/settings/applications/498629
//  https://deadbolt-scala.readme.io/v2.5/docs
  val key = ConsumerKey("a4ea8ede954b7655f7c7", "71d5204d9d9bbe9e9031a7a26e8a0102d06c20f3")
  val oauth = OAuth(ServiceInfo(
    "https://github.com/login/oauth/authorize",
    "https://github.com/login/oauth/access_token",
    "https://api.github.com/user", key),
    true)

  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }



  /**
   * Create an action that responds with the [[Counter]]'s current
   * count. The result is plain text. This `Action` is mapped to
   * `GET /count` requests by an entry in the `routes` config file.
   */
  def count = //checkToken {
    Action {
      val token = "1234"
      val secret = "12345"
      val requestTokeb = RequestToken(token, secret)

      val verifier = "1234"
      oauth.retrieveRequestToken ("http://localhost:9000") match {
//        case Right(r) => oauth.retrieveAccessToken(requestTokeb, "1234") match {
          case Right(t) => {
            // We received the unauthorized tokens in the OAuth object - store it before we proceed
            Redirect(oauth.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
          }

//          case Right(t) => {
//            // We received the authorized tokens in the OAuth object - store it before we proceed
//            Redirect(routes.HomeController.index).withSession("token" -> t.token, "secret" -> t.secret)
//          }
//          case Left(e) => throw e
//        }

        case Left(l) => throw (l)
      }


//      .getOrElse(
//      oauth.retrieveRequestToken("https://localhost:9000/auth") match {
//        case Right(t) => {
//          // We received the unauthorized tokens in the OAuth object - store it before we proceed
//          Redirect(oauth.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
//        }
//        case Left(e) => throw e
//      })

      Ok(counter.nextCount().toString)
    }
  //}

}
