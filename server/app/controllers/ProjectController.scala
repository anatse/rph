package controllers

import java.sql.Timestamp
import java.util.Date
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import model.{ Project, ProjectDAO }
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }
import utils.Logger
import utils.auth.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  pdao: ProjectDAO)(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

  def index = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    logger.warn("insert new project")

    val ins = pdao.insert(Project(
      number = "1",
      name = "test",
      startDate = new Timestamp(new Date().getTime),
      endDate = new Timestamp(new Date().getTime)))

    ins.map(_ => Ok("inserted"))
  }

  implicit val projectWrites = Json.writes[Project]
  def findAll = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    val prjs: Future[Seq[Project]] = pdao.findAll
    //    prjs.map(rows => Ok(Json.obj("projects" -> rows)))
    prjs.map(rows => Ok(views.html.backlog.projects(rows)))
  }

  def create = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    pdao.create.map(p => Ok("created"))
  }
}