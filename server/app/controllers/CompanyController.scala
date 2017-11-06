package controllers

import java.io.File
import java.nio.file.{CopyOption, Files, Paths, StandardCopyOption}
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms.SignInForm
import models._
import models.daos.{CartDAO, DrugsGroupDAO, ProductDAO}
import org.webjars.play.WebJarsUtil
import play.api.Environment
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import utils.JsonUtil
import utils.auth.DefaultEnv

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class CompanyController @Inject()(
  components: ControllerComponents,
  socialProviderRegistry: SocialProviderRegistry,
  silhouette: Silhouette[DefaultEnv],
  drugsProductDAO: ProductDAO,
  drugsGroupDAO: DrugsGroupDAO,
  cartDAO: CartDAO,
  mailerClient: MailerClient,
  env: Environment)(
  implicit
    webJarsUtil: WebJarsUtil,
    ex: ExecutionContext) extends AbstractController(components) with I18nSupport  {

  /**
    * Function generate session UUID if it does not exists or return ones
    * @param session session
    * @return sesion uuid
    */
  private def sessionId (session: Session):String = {
    session.get("uuid").getOrElse {
      java.util.UUID.randomUUID.toString
    }
  }

  /**
    * Function builds main view (shop view)
    * @return
    */
  def view = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = request.identity match {
      case Some(user) => cartDAO.findById(user.userID.toString)
      case None => cartDAO.findById(sid)
    }

    // Add session uuid to current session. This operation performs only in view request
    Future.successful(
      Ok(views.html.shop.shop(SignInForm.form, socialProviderRegistry, request.identity, Await.result(cart, 1 second))).
        withSession(request.session + ("uuid" -> sid))
    )
  }

  def cartView = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = request.identity match {
      case Some(user) => cartDAO.findById(user.userID.toString)
      case None => cartDAO.findById(sid)
    }

    Await.result(cart, 1 second) match {
      case Some(cart) => Future.successful(
        Ok(views.html.shop.cart(SignInForm.form, socialProviderRegistry, request.identity, cart)).withSession(request.session + ("uuid" -> sid))
      )

      case  _ => Future.successful(
        Redirect(routes.CompanyController.view)
      )
    }
  }

  def setImageView = silhouette.UserAwareAction.async { implicit request =>
    val cart = request.identity match {
      case Some(user) => cartDAO.findById(user.userID.toString)
      case None => cartDAO.findById(sessionId(request2session))
    }

    Future.successful(Ok(views.html.shop.image(SignInForm.form, socialProviderRegistry, request.identity, Await.result(cart, 1 second))))
  }

  implicit val productWrites = Json.writes[DrugsProduct]
  implicit val groupWrites = Json.writes[DrugsGroup]
  implicit val groupReads = Json.reads[DrugsGroup]
  implicit val drugsFindRqReads = Json.reads[DrugsFindRq]

  implicit val scitemsWrites = Json.writes[ShopCartItem]
  implicit val scitemsReads = Json.reads[ShopCartItem]

  implicit val cartWrites = Json.writes[ShopCart]
  implicit val cartReads = Json.reads[ShopCart]

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

  def findByGroup = silhouette.UserAwareAction(parse.json[DrugsFindRq]).async { implicit request =>
    val findRq: DrugsFindRq = request.body
    drugsProductDAO.findByGroup(
      group = findRq.groups.getOrElse(Array[String]()),
      text = findRq.text,
      sortField = findRq.sorts,
      offset = findRq.offset,
      pageSize = findRq.pageSize + 1).map(rows => makeResult(rows, findRq.pageSize, findRq.offset))
  }

  def findDrugsProducts(offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.findAll(sort, offset, pageSize+1).map(rows => makeResult(rows, pageSize, offset))
  }

  def combinedSearchDrugsProducts(searchText:String, offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.combinedSearch(searchText, sort, offset, pageSize+1).map(rows => makeResult(rows, pageSize, offset))
  }

  def insertDrugsGroup = silhouette.SecuredAction(parse.json[DrugsGroup]).async { implicit request =>
    val drugsGroup: DrugsGroup = request.body
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

  def addItemToCart = silhouette.UserAwareAction.async { implicit request =>
    val item: ShopCartItem = JsonUtil.fromJson[ShopCartItem](request.body.asText.getOrElse("{}"))

    val cart = request.identity match {
      case Some(user:User) => ShopCart (Some(user.userID.toString), "", Array[ShopCartItem]())
      case _ => ShopCart (None, sessionId(request2session), Array[ShopCartItem]())
    }

    cartDAO.saveItem(cart, item).map(sc => Ok(Json.obj("cart" -> sc)))
  }

  def cartSend = silhouette.UserAwareAction.async { implicit request =>
    val cart = request.identity match {
      case Some(user:User) => ShopCart (Some(user.userID.toString), "", Array[ShopCartItem]())
      case _ => ShopCart (None, sessionId(request2session), Array[ShopCartItem]())
    }

    val email = request.body.asFormUrlEncoded.get("email").head

    cartDAO.findById(cart.userId.getOrElse(cart.sessionId)).flatMap(
      sc => Future (
        mailerClient.send(Email(
          subject = Messages("email.order.title"),
          from = email,
          to = Seq(Messages("email.order.perform")),
          bodyText = Some(views.txt.emails.cart(sc.get).body),
          bodyHtml = Some(views.html.emails.cart(sc.get).body)))
      )
    ).map(_ => Redirect(routes.CompanyController.view))
  }
}
