package controllers

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
    * Function builds main view (shop view)
    * @return
    */
  def shopView = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, request.identity))

    // Add session uuid to current session. This operation performs only in view request
    Future.successful(
      Ok(views.html.shop.shop(SignInForm.form, socialProviderRegistry, request.identity, Await.result(cart, 1 second))).
        withSession(request.session + ("uuid" -> sid))
    )
  }

  def cartView = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, request.identity))

    Await.result(cart, 1 second) match {
      case Some(cart) => Future.successful(
        Ok(views.html.shop.cart(SignInForm.form, socialProviderRegistry, request.identity, cart)).withSession(request.session + ("uuid" -> sid))
      )

      case  _ => Future.successful(
        Redirect(routes.CompanyController.shopView)
      )
    }
  }

  implicit val productWrites = Json.writes[DrugsProduct]
  implicit val producRsWrites = Json.writes[DrugsProdRs]
  implicit val productRqWrites = Json.writes[DrugsFindRq]
  implicit val groupWrites = Json.writes[DrugsGroup]
  implicit val groupReads = Json.reads[DrugsGroup]
  implicit val drugsFindRqReads = Json.reads[DrugsFindRq]

  implicit val scitemsWrites = Json.writes[ShopCartItem]
  implicit val scitemsReads = Json.reads[ShopCartItem]

  implicit val cartWrites = Json.writes[ShopCart]
  implicit val cartReads = Json.reads[ShopCart]

  implicit val recProductReads = Json.reads[RecommendedDrugs]
  implicit val recProductWrites = Json.writes[RecommendedDrugs]

  protected def makeResult (rows:List[DrugsProduct], realPageSize:Int, offset:Int) = {
    val filterredRows = if (rows.length > realPageSize) rows.dropRight(1) else rows
    Ok(Json.obj("rows" -> filterredRows, "pageSize" -> realPageSize, "offset" -> offset, "hasMore" -> (rows.length > realPageSize)))
  }

  protected def makeResultRS (rows:List[DrugsProdRs], realPageSize:Int, offset:Int) = {
    val filterredRows = if (rows.length > realPageSize) rows.dropRight(1) else rows
    Ok(Json.obj("rows" -> filterredRows, "pageSize" -> realPageSize, "offset" -> offset, "hasMore" -> (rows.length > realPageSize)))
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

  def addCartInfo(frows:Future[List[DrugsProduct]]):ShopCart => Future[List[DrugsProdRs]] = {
    sc => cartDAO.find(sc).flatMap(
      cart => cart match {
        case Some(c) =>
          frows.map (rows => rows.map (
            row => c.items.find(_.drugId == row.id) match {
                case Some(c) => DrugsProdRs(row, countInCart = c.num)
                case _ => DrugsProdRs(row, countInCart = 0)
              }
            )
          )

        case _ => frows.map (rows => rows.map (row => DrugsProdRs(row, countInCart = 0)))
      })
  }

  def findDrugsProducts(offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    val cart = getCart(sessionId(request2session), request.identity)
    addCartInfo (drugsProductDAO.findAll(sort, offset, pageSize+1))(cart).map(
      rows => makeResultRS(rows, pageSize, offset)
    )
  }

  def findRecommended(offset:Int, pageSize:Int) = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.findRecommended(offset, pageSize).map(rows => Ok(Json.obj("rows" -> rows)))
  }

  def combinedSearchDrugsProducts(searchText:String, offset:Int, pageSize:Int, sort:Option[String] = None) = silhouette.UserAwareAction.async { implicit request =>
    val cart = getCart(sessionId(request2session), request.identity)
    addCartInfo (drugsProductDAO.combinedSearch(searchText, sort, offset, pageSize+1))(cart).map(
      rows => makeResultRS(rows, pageSize, offset)
    )
  }

  def addItemToCart = silhouette.UserAwareAction.async { implicit request =>
    val item: ShopCartItem = JsonUtil.fromJson[ShopCartItem](request.body.asText.getOrElse("{}"))
    val cart = getCart(sessionId(request2session), request.identity)
    cartDAO.saveItem(cart, item).flatMap(sc => cartDAO.find(cart)).map(c => Ok(Json.obj("cart" -> c)))
  }

  def cartSend = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = getCart(sid, request.identity)

    val email = request.body.asFormUrlEncoded.get("email").head

    cartDAO.find(cart).flatMap(
      sc => Future {
        mailerClient.send(Email(
          subject = Messages("email.order.title"),
          from = email,
          to = Seq(Messages("email.order.perform")),
          bodyText = Some(views.txt.emails.cart(sc.get).body),
          bodyHtml = Some(views.html.emails.cart(sc.get).body)))

        mailerClient.send(Email(
          subject = Messages("email.order.title"),
          from = Messages("contact.email"),
          to = Seq(email),
          bodyText = Some(views.txt.emails.cart(sc.get).body),
          bodyHtml = Some(views.html.emails.cart(sc.get).body)))
      }
    ).map(_ => Redirect(routes.CompanyController.shopView))
  }
}
