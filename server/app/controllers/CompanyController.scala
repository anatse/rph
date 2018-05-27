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
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}
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
    ex: ExecutionContext) extends AbstractController(components) with I18nSupport with ModelImplicits  {

  /**
    * Function adds information from cart to products item for each item row
    * @param frows products items
    * @return products with cart information
    */
  private def addCartInfo(frows:Future[List[DrugsProduct]]):ShopCart => Future[List[DrugsProdRs]] = {
    sc => cartDAO.find(sc).flatMap(
      cart => cart match {
        case Some(c) =>
          frows.map (rows => rows.map (
            row => c.items.find(_.drugId == row.id) match {
              case Some(c) => DrugsProdRs(row, countInCart = c.num)
              case _ => DrugsProdRs(row, countInCart = 0)
            }
          ))

        case _ => frows.map (rows => rows.map (row => DrugsProdRs(row, countInCart = 0)))
      })
  }

  /**
    * Function builds main view (shop view)
    * @return
    */
  def shopView = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, request.identity))

    // Add session uuid to current session. This operation performs only in view request
    cart.map(c => Ok(views.html.shop.shop(SignInForm.form, socialProviderRegistry, request.identity, c)).withSession(request.session + ("uuid" -> sid)))
  }

  /**
    * Function builds cart view
    * @return
    */
  def cartView = silhouette.UserAwareAction.async { implicit request =>
    val sid = sessionId(request2session)
    val cart = cartDAO.find(getCart(sid, request.identity))
    cart.map ( c => c match {
      case Some(cart) => Ok(views.html.shop.cart(SignInForm.form, socialProviderRegistry, request.identity, cart)).withSession(request.session + ("uuid" -> sid))
      case  _ => Redirect(routes.CompanyController.shopView)
    })
  }

  /**
    * Function retrieves all product groups. Product groups has only one level
    * @return product groups
    */
  def findDrugsGroups = silhouette.UserAwareAction.async { implicit request =>
    drugsGroupDAO.findAll(None, 0, 0).map(rows => Ok(Json.obj("rows" -> rows)))
  }

  def combinedSearchDrugsProducts = silhouette.UserAwareAction(parse.json[DrugsFindRq]).async { implicit request =>
    val cart = getCart(sessionId(request2session), request.identity)
    val find = request.body
    if (find.text.getOrElse("") == "") {
      addCartInfo(drugsProductDAO.findAll(None, find.offset, find.pageSize))(cart).map(
        rows => makeResultRS(rows, find.pageSize, find.offset)
      )
    }
    else
      addCartInfo (drugsProductDAO.combinedSearch(find.copy(pageSize = find.pageSize + 1)))(cart).map(
        rows => makeResultRS(rows, find.pageSize, find.offset)
      )
  }

  def addItemToCart = silhouette.UserAwareAction(parse.json[ShopCartItem]).async { implicit request =>
    val item: ShopCartItem = request.body //JsonUtil.fromJson[ShopCartItem](request.body.asText.getOrElse("{}"))
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

  /**
    * Send email about order brom remote cart. Used by android application
    * @return
    */
  def remoteCartSend = silhouette.UserAwareAction(parse.json[List[RemoteCart]]).async { implicit request =>
    val cart = request.body
    val bodyHtml = views.html.emails.remoteCart(sc.get).body
    val orderNum = Math.abs(bodyHtml.hashCode() % 1000000)

    Future {
      mailerClient.send(Email(
        subject = s"$orderNum: ${Messages("email.order.title")}",
        from = Messages("email.from"),
        to = Seq(Messages("email.order.perform")),
        bodyHtml = Some(views.html.emails.remoteCart(sc.get).body)
      ))
    }.map(c => Ok(Json.obj("orderNo" -> orderNum)))
  }
}
