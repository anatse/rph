package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.DrugsProduct
import models.daos.ProductDAO
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{JsonUtil, Logger}
import utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Success

class GoodsImportController @Inject()(
    components: ControllerComponents,
    silhouette: Silhouette[DefaultEnv],
    productDAO: ProductDAO)(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

//  def uploadNonSecured = Action(parse.multipartFormData).async { request =>
//    Future {
//      request.body.file("data").map { picture =>
//        val filename = picture.filename
//        val contentType = picture.contentType
//
//        println(s"file: ${picture.ref.getAbsolutePath}, realPath: ${picture.ref.path.toRealPath()}")
//
//        Source.fromFile(picture.ref.path.toString).foreach {
//          print
//        }
//
//        Ok("File uploaded")
//      }.get
//    }
//  }

  val json = "[{\"RetailPrice\":\"249.42\",\"BarCode\":\"1111222333444\",\"OstFirst\":\"2\",\"TradeTech\":\"ХЗ\",\"ProducerFullName\":\"ОАО Нанофарм\",\"DrugsFullName\":\"Аспирин\",\"SupplierFullName\":\"Катрен\",\"MNN\":\"ацетилсалициловая кислота\",\"Ost\":\"1\",\"UnitFullName\":\"упаковка\",\"ProducerShortName\":\"Нанофарм\",\"DrugFullName\":\"Аспирин-С\",\"OstLast\":\"1\",\"DrugsShortName\":\"Аспирин\",\"Packaging\":\"упаковки\",\"ID\":\"10001\",\"UnitShortName\":\"упак.\"}]";

  def upload = silhouette.SecuredAction (parse.multipartFormData).async { request =>
    val drugsToSave = request.body.file("data").map { picture =>
      val filename = picture.filename
      val contentType = picture.contentType
      val fileText = Source.fromFile(picture.ref.path.toString).mkString

      val mapValues = JsonUtil.fromJson[List[Map[String, Option[String]]]](fileText);
      mapValues.map (v => {
        DrugsProduct (
          retailPrice = v("RetailPrice").getOrElse("0").toDouble,
          barCode = v("BarCode").getOrElse(""),
          ostFirst = v("OstFirst").getOrElse("0").toDouble,
          tradeTech = v("TradeTech").getOrElse(""),
          producerFullName = v("ProducerFullName").getOrElse(""),
          drugsFullName = v("DrugsFullName").getOrElse(""),
          supplierFullName = v("SupplierFullName").getOrElse(""),
          MNN = v("MNN").getOrElse(""),
          ost = v("Ost").getOrElse("0").toDouble,
          unitFullName = v("UnitFullName").getOrElse(""),
          producerShortName = v("ProducerShortName").getOrElse(""),
          drugFullName = v("DrugFullName").getOrElse(""),
          ostLast = v("OstLast").getOrElse("0").toDouble,
          drugsShortName = v("DrugsShortName").getOrElse(""),
          packaging = v("Packaging").getOrElse(""),
          id = v("ID").getOrElse(""),
          unitShortName = v("UnitShortName").getOrElse("")
        )
      })
    }

    println("drugsTOSave: " + drugsToSave)

    drugsToSave match {
      case Some(drugs) => productDAO.bulkInsert(drugs).map(_ => Ok("uploaded"))
      case _ => Future(Ok("No drugs provided"))
    }
  }
}
