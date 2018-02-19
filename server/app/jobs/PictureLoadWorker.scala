package jobs

import java.net.{HttpURLConnection, URL}

import akka.actor.{Actor, Props}
import com.cloudinary.{Cloudinary, Transformation}
import jobs.PictureLoadWorker.SetImage
import models.DrugsProduct
import models.daos.ProductDAO
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import utils.Logger
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class PictureLoadWorker(cloudinary: Cloudinary, productDAO: ProductDAO) extends Actor with Logger {
  val browser = JsoupBrowser()

  private final def prepareSearch(drug: String): String = {
    val parts = drug.split(" ")
    if (parts.length > 1) {
      s"${parts(0)}+${parts(1)}"
    }
    else
      drug
  }

  def loadImage(imageUrl: String): Array[Byte] = {
    logger.info(s"imageUrl: $imageUrl")
    val url = new URL(imageUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val is = connection.getInputStream
    Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
  }

  private final def setImage (si: SetImage) = {
    import scala.collection.JavaConverters._

    logger.info(s"Trying to save image for ${si.drugId} length: ${si.imgData.length}")

    val uploadInfo = cloudinary.uploader().upload(si.imgData, Map("folder" -> "drugs", "public_id" -> s"${si.drugId}").asJava)
    var trx = new Transformation()
    trx = trx.width(230)
    trx = trx.height(118)
    val url = cloudinary.url().secure(true).format("jpg")
      .transformation(trx.crop("fit"))
      .generate(s"drugs/${si.drugId}");

    logger.info(s"Trying to set image for ${si.drugId}")
    import context.dispatcher
    productDAO.addImage(si.drugId, url).onComplete {
      case Success(_) => logger.info("Successfully set image")
      case Failure(e) => logger.error("Error set image", e)
    }
  }

  override def receive: Receive = {
    case dp:DrugsProduct =>
      if (!dp.drugImage.isDefined) {
        logger.info(s"Trying to load picture for drugID ${dp.drugsID} ...")
        val searchUrl = s"https://apteka.ru/search/?q=${prepareSearch(dp.drugsShortName)}"
        logger.info(s"Trying to connect to ${searchUrl}")
        val doc = browser.get(searchUrl)
        val items = doc >> elementList("div.b-description_product figure a")
        items.foreach {
          item =>
            val childUrl = s"https://apteka.ru${item.attr("href")}"
            logger.info(s"load dependent url with big image ${childUrl}")
            val imgDoc = browser.get(childUrl)
            val img = imgDoc >> element ("figure > img")
            val imgData = loadImage (s"https://apteka.ru${img.attr("src")}")
            setImage (SetImage(dp.drugsID, imgData))
        }
      }
      else {
        logger.info(s"Picture for drugID ${dp.drugsID} already exists. Worker won't be change it")
      }

    case e => logger.error("Unknown message")
  }
}

object PictureLoadWorker {
  case class SetImage (drugId: String, imgData: Array[Byte])

  // Recommended
  def props(cloudinary: Cloudinary, productDAO: ProductDAO):Props = Props(new PictureLoadWorker(cloudinary, productDAO))
  // Unrecommended
  def props1(cloudinary: Cloudinary, productDAO: ProductDAO):Props = Props(classOf[PictureLoadWorker], cloudinary, productDAO)
}

