package controllers

import java.io.{File, FileFilter}
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
 )(implicit webJarsUtil: WebJarsUtil, ex: ExecutionContext) extends AbstractController(components) with I18nSupport with ModelImplicits {

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
        val folderPath = getImagedFolder
        val imagePath = s"${folderPath.getAbsolutePath}/${id}.jpg"
        val imageName = s"${id}.jpg"
        val fp = new File (imagePath)

        Files.copy(file.ref.path, fp.toPath, StandardCopyOption.REPLACE_EXISTING)
        drugsProductDAO.addImage(id, imageName).map (row => Ok(Json.obj("res" -> row)))

      case _ => Future.successful(Ok("Error loading file. File is empty"))
    }
  }

  def getImagedFolder = new File("/app/server/public/images/")

  def downloadAllImages = silhouette.SecuredAction.async { request =>
    Future {
//      val folderPath = env.getFile("/public/images")
      val folderPath = getImagedFolder

      import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
      import java.util.zip.{ZipEntry, ZipOutputStream}

      val zipFile = s"${folderPath.getAbsolutePath}/images.zip"
      val zip = new ZipOutputStream(new FileOutputStream(zipFile))

      folderPath.listFiles(new FileFilter() {
        override def accept(pathname: File) = {
          val pat = ".*\\.(jpg|png|jpeg|bmp)$".r
          !pathname.isDirectory && !pat.findFirstIn(pathname.getName).isEmpty
        }
      }).foreach { file =>
        val fileName = file.getName
        zip.putNextEntry(new ZipEntry(fileName))
        val in = new BufferedInputStream(new FileInputStream(file))
        var b = in.read()
        while (b > -1) {
          zip.write(b)
          b = in.read()
        }

        println(s"added file: ${fileName}")
        in.close()
        zip.closeEntry()
      }
      zip.close()

      Ok.sendFile(
        content = new java.io.File(zipFile),
        fileName = _ => "images.zip"
      )
    }
  }

  def filterProducts = silhouette.SecuredAction(parse.json[DrugsAdminRq]).async { implicit request =>
    drugsProductDAO.getAll(request.body).map (rows => Ok(Json.obj("rows" -> rows)))
  }

  def findRecommended = silhouette.UserAwareAction.async { implicit request =>
    drugsProductDAO.findRecommended.map(rows => Ok(Json.obj("rows" -> rows)))
  }

  def addRecommended (drugId: String, orderNum: Int) = silhouette.SecuredAction(WithRoles("ADMIN")).async {
    request => drugsProductDAO.addRecommended(drugId, orderNum).map(_ => Ok("OK"))
  }

  def removeRecommended (drugId: String) = silhouette.SecuredAction(WithRoles("ADMIN")).async {
    request => drugsProductDAO.removeRecommended(drugId).map(_ => Ok("OK"))
  }
}
