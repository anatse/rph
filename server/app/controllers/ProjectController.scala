package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api.{Authorization, LoginInfo, Silhouette}
import org.webjars.play.WebJarsUtil
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import utils.Logger
import utils.auth.DefaultEnv

class ProjectController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv])(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

//  def index = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
////    Await.result(pdao.dropAll, 1 second)
////    Await.result(pdao.create, 1 second)
//
//    for (i <- 1 to 9) {
//      val ins = pdao.insert(Project(
//        number = s"$i",
//        name = s"test_$i",
//        description = Some(s"description for projects test $i"),
//        startDate = Some(new Timestamp(new Date().getTime)))
//      )
//
//      Await.result(ins.map(v => logger.warn(s"inserted: ${v}")), 1 second)
//    }
//
//    Future {Ok("inserted")}
//  }
//
//  case class WithCheck() extends Authorization[User, CookieAuthenticator] {
//    override def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request: Request[B]) = {
//      println(s"user: $user")
//      println(s"user: $request")
//      Future.successful(!user.email.isEmpty)
//    }
//  }
//
//  val DEFAULT_PAGE_SIZE = 3
//  implicit val projectWrites = Json.writes[Project]
//  def findAll(offset: Option[Int], pageSize: Option[Int]) = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
//    val realPageSize = pageSize.getOrElse(DEFAULT_PAGE_SIZE)
//    val prjs = pdao.findAll(pageSize = Some(realPageSize + 1), offset = offset)
//    prjs.map(rows => {
//      val filterredRows = if (rows.length > realPageSize) rows.dropRight(1) else rows
//      Ok(views.html.rph.projects(filterredRows, offset.getOrElse(0), realPageSize, rows.length > realPageSize, request.identity))
//    })
//  }
//
//  def jsonFindAll(offset: Option[Int], pageSize: Option[Int]) = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
//    val realPageSize = pageSize.getOrElse(DEFAULT_PAGE_SIZE)
//    val prjs = pdao.findAll(pageSize = Some(realPageSize + 1), offset = offset)
//    prjs.map(rows => {
//      val filterredRows = if (rows.length > realPageSize) rows.dropRight(1) else rows
//      Ok(Json.obj("rows" -> filterredRows, "pageSize" -> realPageSize, "offset" -> offset, "hasMore" -> (rows.length > realPageSize)))
//    })
//  }
//
//  def project(projectId: Long) = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
//    val prjs: Future[Project] = pdao.findById (projectId)
//    prjs.map(project => Ok(views.html.backlog.project(project, request.identity)))
//  }
//
//  def create = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
//    pdao.create.map(p => Ok("created"))
//  }
}
