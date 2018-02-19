package controllers

import java.io.{File, FileFilter}
import java.nio.file.{CopyOption, Files, Paths, StandardCopyOption}
import javax.inject.Inject

import akka.actor.ActorRef
import com.cloudinary.{Cloudinary, Transformation}
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms.SignInForm
import jobs.PictureLoader.{LoadAll, LoadByName}
import models.{ShopCart, _}
import models.daos.{CartDAO, DrugsGroupDAO, ProductDAO}
import models.services.WithRoles
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Environment}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.{request, _}
import utils.JsonUtil
import utils.auth.DefaultEnv

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import utils.CartUtil._

import scala.util.{Failure, Success, Try}


class AdminController @Inject()(
   components: ControllerComponents,
   socialProviderRegistry: SocialProviderRegistry,
   silhouette: Silhouette[DefaultEnv],
   drugsProductDAO: ProductDAO,
   drugsGroupDAO: DrugsGroupDAO,
   cartDAO: CartDAO,
   mailerClient: MailerClient,
   cloudinary: Cloudinary,
   @Named("picture-loader") pictureLoader: ActorRef,
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

  /**
    *
    * @param drugName
    * @return
    */
  def getPicturesFromAptru(drugName:String) = silhouette.SecuredAction.async { implicit request =>
    // Call uploadAll pictures
    pictureLoader ! LoadByName (drugName)
    Future.successful(Ok("process started"))
  }

  def setImageToDrug = silhouette.SecuredAction (parse.multipartFormData).async { request =>
    import scala.collection.JavaConverters._
    val image = request.body.file("image")
    val urlParam = Try(request.body.dataParts("image_url")(0))
    val id = request.body.dataParts("id")(0)
    image match {
      case Some(file) =>
        val bytes = Files.readAllBytes(file.ref.path)
        val uploadInfo = cloudinary.uploader().upload(bytes, Map("folder" -> "drugs", "public_id" -> s"${id}").asJava)
        var trx = new Transformation()
        trx = trx.width(230)
        trx = trx.height(118)
        val url = cloudinary.url().secure(true).format("jpg")
          .transformation(trx.crop("fit"))
          .generate(s"drugs/${id}");

        drugsProductDAO.addImage(id, url).map (row => Ok(Json.obj("res" -> row)))

      case _ => urlParam match {
        case Success(url) =>
          drugsProductDAO.addImage(id, url).map (row => Ok(Json.obj("res" -> row)))

        case Failure(err) => Future.successful(Ok(Json.obj("error" -> err.getMessage)))
      }
    }
  }

  def downloadAllImages = silhouette.SecuredAction.async { request =>
    Future {
      val folderPath = env.getFile("/public/images")

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
