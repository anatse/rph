package controllers

import java.net.URLEncoder
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import forms.SignInForm
import models.services.UserService
import net.ceedubs.ficus.Ficus._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import utils.auth.{DefaultEnv, JWTEnv}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Sign In` controller.
 *
 * @param components             The Play controller components.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param credentialsProvider    The credentials provider.
 * @param socialProviderRegistry The social provider registry.
 * @param configuration          The Play configuration.
 * @param clock                  The clock instance.
 * @param webJarsUtil            The webjar util.
 */
class SignInController @Inject() (
     components: ControllerComponents,
     silhouette: Silhouette[DefaultEnv],
     userService: UserService,
     credentialsProvider: CredentialsProvider,
     socialProviderRegistry: SocialProviderRegistry,
     configuration: Configuration,
     clock: Clock)(
  implicit
  webJarsUtil: WebJarsUtil,
  ex: ExecutionContext) extends AbstractController(components) with I18nSupport {


  /**
   * Handles the submitted form.
   * http://localhost:9000/account/activate/73ad970b-82ce-4484-8f47-4c5d1be5db1c
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignInForm.form.bindFromRequest.fold(
      form => {
        Future.successful(BadRequest(""))
      },
      data => {
        val credentials = Credentials(data.email, data.password)
        val auth = credentialsProvider.authenticate(credentials)
        val redirectUrl = data.redirectUrl

        auth.flatMap { loginInfo =>
          val result = Redirect(redirectUrl)
          userService.retrieve(loginInfo).flatMap {
            case Some(user) if !user.activated =>
              Future.successful(Ok(views.html.activateAccount(data.email, SignInForm.form, socialProviderRegistry)))
            case Some(user) =>
              val c = configuration.underlying
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator if data.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, result)
                }
              }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case ex: ProviderException =>
            ex.printStackTrace()
            Redirect(redirectUrl).flashing("error" -> Messages("invalid.credentials"))
        }
      })
  }
}
