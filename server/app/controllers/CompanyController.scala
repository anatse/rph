package controllers

import java.io.File
import java.nio.file.{CopyOption, Files, Paths, StandardCopyOption}
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms.SignInForm
import models.{DrugsGroup, DrugsProduct, ShopCart}
import models.daos.{DrugsGroupDAO, ProductDAO}
import org.webjars.play.WebJarsUtil
import play.api.Environment
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class CompanyController @Inject()(
  components: ControllerComponents,
  socialProviderRegistry: SocialProviderRegistry,
  silhouette: Silhouette[DefaultEnv],
  drugsProductDAO: ProductDAO,
  drugsGroupDAO: DrugsGroupDAO,
  env: Environment)(
  implicit
    webJarsUtil: WebJarsUtil,
    ex: ExecutionContext) extends AbstractController(components) with I18nSupport  {

  def view = silhouette.UserAwareAction.async { implicit request =>
    val shopCart = ShopCart ("test", null)
    Future.successful(Ok(views.html.shop.shop(SignInForm.form, socialProviderRegistry, request.identity, Some(shopCart))))
  }

  def setImageView = silhouette.UserAwareAction.async { implicit request =>
    val shopCart = ShopCart ("test", null)
    Future.successful(Ok(views.html.shop.image(SignInForm.form, socialProviderRegistry, request.identity, None)))
  }

  implicit val productWrites = Json.writes[DrugsProduct]
  implicit val groupWrites = Json.writes[DrugsGroup]

  protected def makeResult (rows:List[DrugsProduct], realPageSize:Int, offset:Int) = {
    val filterredRows = if (rows.length > realPageSize) rows.dropRight(1) else rows
    Ok(Json.obj("rows" -> filterredRows, "pageSize" -> realPageSize, "offset" -> offset, "hasMore" -> (rows.length > realPageSize)))
  }

  def create = silhouette.SecuredAction.async { implicit request =>
    drugsProductDAO.createTextIndex().map(_ => Ok("OK"))
  }

  def findDrugsGroups = silhouette.UserAwareAction.async { implicit request =>
    drugsGroupDAO.findAll(None, 0, 0).map(rows => Ok(Json.obj("rows" -> rows)))
  }

  def findDrugsProducts(offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.findAll(sort, offset, pageSize+1).map(rows => makeResult(rows, pageSize, offset))
  }

  def combinedSearchDrugsProducts(searchText:String, offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.combinedSearch(searchText, sort, offset, pageSize+1).map(rows => makeResult(rows, pageSize, offset))
  }

  def insertDrugsGroup(drugsGroup: DrugsGroup) = silhouette.SecuredAction.async { implicit request =>
    drugsGroupDAO.save(drugsGroup).map(rows => Ok(Json.obj("rows" -> rows)))
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
