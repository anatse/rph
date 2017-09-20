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

      val mapValues = JsonUtil.fromJson[List[Map[String, String]]](json);
      mapValues.map (v => {
        DrugsProduct (
          retailPrice = v("RetailPrice").toDouble,
          barCode = v("BarCode"),
          ostFirst = v("OstFirst").toDouble,
          tradeTech = v("TradeTech"),
          producerFullName = v("ProducerFullName"),
          drugsFullName = v("DrugsFullName"),
          supplierFullName = v("SupplierFullName"),
          MNN = v("MNN"),
          ost = v("Ost").toDouble,
          unitFullName = v("UnitFullName"),
          producerShortName = v("ProducerShortName"),
          drugFullName = v("DrugFullName"),
          ostLast = v("OstLast").toDouble,
          drugsShortName = v("DrugsShortName"),
          packaging = v("Packaging"),
          id = v("ID"),
          unitShortName = v("UnitShortName")
        )
      })
    }

    drugsToSave match {
      case Some(drugs) => productDAO.bulkInsert(drugsToSave.getOrElse(List.empty)).map(_ => Ok("uploaded"))
      case _ => Future(Ok("No drugs provided"))
    }
  }
}
