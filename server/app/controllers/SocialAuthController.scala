package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import models.services.UserService
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

/**
 * The social auth controller.
 *
 * @param components             The Play controller components.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 * @param ex                     The execution context.
 */
class SocialAuthController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  socialProviderRegistry: SocialProviderRegistry)(
  implicit
  ex: ExecutionContext) extends AbstractController(components) with I18nSupport with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request: Request[AnyContent] =>
    val sourceUrl = request2session.get("sourceUrl")

    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- p.retrieveProfile(authInfo)
            user <- userService.save(profile)
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
            value <- silhouette.env.authenticatorService.init(authenticator)
//          JWT Implementation
//            result <- silhouette.env.authenticatorService.embed(value, Ok(Json.obj("token" -> value)))
            result <- silhouette.env.authenticatorService.embed(value, Redirect(sourceUrl.getOrElse(routes.CompanyController.shopView().url)).withHeaders("X-Auth-Token" -> s"$value"))
          } yield {
            //println (s"profile: ${profile}\nuser: ${user}\nauthInfo: ${authInfo}\nauthenticator: ${authenticator}\nvallue: ${value}\nresult: ${result}")
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            result
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.CompanyController.shopView()).flashing("error" -> Messages("could.not.authenticate"))
    }
  }
}
