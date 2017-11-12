package controllers

import java.io.File
import java.nio.file.{CopyOption, Files, Paths, StandardCopyOption}
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms.SignInForm
import models.{ShopCart, _}
import models.daos.{CartDAO, DrugsGroupDAO, ProductDAO}
import org.webjars.play.WebJarsUtil
import play.api.Environment
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.{request, _}
import utils.JsonUtil
import utils.auth.DefaultEnv

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import utils.CartUtil._


class AdminController @Inject()(
  components: ControllerComponents,
  socialProviderRegistry: SocialProviderRegistry,
  silhouette: Silhouette[DefaultEnv],
  drugsProductDAO: ProductDAO,
  drugsGroupDAO: DrugsGroupDAO,
  cartDAO: CartDAO,
  mailerClient: MailerClient,
  env: Environment
 )(implicit webJarsUtil: WebJarsUtil, ex: ExecutionContext) extends AbstractController(components) with I18nSupport  {

  implicit val groupWrites = Json.writes[DrugsGroup]
  implicit val groupReads = Json.reads[DrugsGroup]
  implicit val productWrites = Json.writes[DrugsProduct]

  def adminView = silhouette.SecuredAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, Some(request.identity)))

    if (request.identity.roles.getOrElse(Array()).contains("ADMIN")) {
      Future.successful(Ok(views.html.shop.admin(SignInForm.form, socialProviderRegistry, Some(request.identity), Await.result(cart, 1 second))))
    }
    else  {
      Future.successful(Redirect(routes.CompanyController.shopView()).flashing("error" -> Messages("insufficient_prifilegies")))
    }
  }

  def setImageView = silhouette.SecuredAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, Some(request.identity)))

    Future.successful(Ok(views.html.shop.image(SignInForm.form, socialProviderRegistry, Some(request.identity), Await.result(cart, 1 second))))
  }

  def insertDrugsGroup = silhouette.SecuredAction(parse.json[DrugsGroup]).async { implicit request =>
    val drugsGroup: DrugsGroup = request.body
    drugsGroupDAO.save(drugsGroup).map(rows => Ok(Json.obj("rows" -> rows)))
  }

  def create = silhouette.SecuredAction.async { implicit request =>
    drugsProductDAO.createTextIndex().map(_ => Ok("OK"))
  }

  def setImageToDrug = silhouette.SecuredAction (parse.multipartFormData).async { request =>
    val image = request.body.file("image")
    val id = request.body.dataParts("id")(0)
    image match {
      case Some(file) =>
        val folderPath = env.getFile("/public/images");
        val imagePath = s"${folderPath.getAbsolutePath}/${id}.jpg"
        val imageName = s"${id}.jpg"
        val fp = new File (imagePath)
        Files.copy(file.ref.path, fp.toPath, StandardCopyOption.REPLACE_EXISTING)
        drugsProductDAO.addImage(id, imageName).map (row => Ok(Json.obj("res" -> row)))

      case _ => Future.successful(Ok("Error loading file. File is empty"))
    }
  }
}
