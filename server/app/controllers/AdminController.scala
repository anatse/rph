package controllers

import java.io.File
import java.nio.file.{CopyOption, Files, Paths, StandardCopyOption}
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms.SignInForm
import models.{ShopCart, _}
import models.daos.{CartDAO, DrugsGroupDAO, ProductDAO}
import models.services.WithRoles
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
  implicit val dfrReads = Json.reads[DrugsFindRq]

  private def makeResult (rows:List[DrugsProduct], realPageSize:Int, offset:Int) = {
    val filterredRows = if (rows.length > realPageSize) rows.dropRight(1) else rows
    Ok(Json.obj("rows" -> filterredRows, "pageSize" -> realPageSize, "offset" -> offset, "hasMore" -> (rows.length > realPageSize)))
  }

  def adminView = silhouette.SecuredAction(WithRoles("ADMIN")).async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, Some(request.identity)))

    Future.successful(Ok(views.html.shop.admin(SignInForm.form, socialProviderRegistry, Some(request.identity), Await.result(cart, 1 second))))
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

  def filterProducts = silhouette.SecuredAction(parse.json[DrugsFindRq]).async { implicit request =>
    val drugsFindRq: DrugsFindRq = request.body
    drugsProductDAO.filter(drugsFindRq.copy(pageSize = drugsFindRq.pageSize + 1)).map(rows => makeResult(rows, drugsFindRq.pageSize, drugsFindRq.offset))
  }

  def addRecommended (drugId: String, orderNum: Int) = silhouette.SecuredAction(WithRoles("ADMIN")).async {
    request => drugsProductDAO.addRecommended(drugId, orderNum).map(_ => Ok("OK"))
  }

  def removeRecommended (drugId: String) = silhouette.SecuredAction(WithRoles("ADMIN")).async {
    request => drugsProductDAO.removeRecommended(drugId).map(_ => Ok("OK"))
  }
}
