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
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future }

class ProjectController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  pdao: ProjectDAO)(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

  def index = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    val ins = pdao.insert(Project(
      number = "1",
      name = "test",
      startDate = Some(new Timestamp(new Date().getTime)))
    )

    ins.map(_ => Ok("inserted"))
  }

  implicit val projectWrites = Json.writes[Project]
  def findAll(offset:Option[Int]) = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    System.out.println("findAll: " + offset)
    val prjs: Future[Seq[Project]] = pdao.findAll(Some(3), offset)
    prjs.map(rows => Ok(views.html.rph.projects(rows, offset.getOrElse(0), request.identity)))
  }

  def project(projectId: Long) = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    val prjs: Future[Project] = pdao.findById (projectId)
    prjs.map(project => Ok(views.html.backlog.project(project, request.identity)))
  }

  def create = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    pdao.create.map(p => Ok("created"))
  }
}
