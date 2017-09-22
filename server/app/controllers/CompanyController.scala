package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import forms.SignInForm
import models.{DrugsGroup, DrugsProduct}
import models.daos.{DrugsGroupDAO, ProductDAO}
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class CompanyController @Inject()(
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  drugsProductDAO: ProductDAO,
  drugsGroupDAO: DrugsGroupDAO)(
  implicit
    webJarsUtil: WebJarsUtil,
    ex: ExecutionContext) extends AbstractController(components) with I18nSupport  {

  def view = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.shop.main("Hello", request.identity)))
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
    drugsProductDAO.findAll(sort, offset, pageSize).map(rows => makeResult(rows, pageSize, offset))
  }

  def searchDrugsProducts(searchText:String, offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.textSearch(searchText, sort, offset, pageSize).map(rows => makeResult(rows, pageSize, offset))
  }

  def combinedSearchDrugsProducts(searchText:String, offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.combinedSearch(searchText, sort, offset, pageSize).map(rows => makeResult(rows, pageSize, offset))
  }

  def insertDrugsGroup(drugsGroup: DrugsGroup) = silhouette.SecuredAction.async { implicit request =>
    drugsGroupDAO.save(drugsGroup).map(rows => Ok(Json.obj("rows" -> rows)))
  }
}
