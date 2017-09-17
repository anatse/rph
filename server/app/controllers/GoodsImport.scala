package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.DrugsProduct
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{JsonUtil, Logger}
import utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class GoodsImport @Inject()(
    components: ControllerComponents,
    silhouette: Silhouette[DefaultEnv])(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

  def uploadNonSecured = Action(parse.multipartFormData).async { request =>
    Future {
      request.body.file("data").map { picture =>
        val filename = picture.filename
        val contentType = picture.contentType

        println(s"file: ${picture.ref.getAbsolutePath}, realPath: ${picture.ref.path.toRealPath()}")

        Source.fromFile(picture.ref.path.toString).foreach {
          print
        }

        Ok("File uploaded")
      }.get
    }
  }

  def upload = silhouette.SecuredAction(parse.multipartFormData).async { request =>
    request.body.file("data").map { picture =>
      val filename = picture.filename
      val contentType = picture.contentType
      println(s"file: ${picture.ref.path}, realPath: ${picture.ref.path.toRealPath()}")

      val fileText = Source.fromFile(picture.ref.path.toString).mkString

      val json = "[{\"RetailPrice\":\"249.42\",\"BarCode\":\"1111222333444\",\"OstFirst\":\"2\",\"TradeTech\":\"ХЗ\",\"ProducerFullName\":\"ОАО Нанофарм\",\"DrugsFullName\":\"Аспирин\",\"SupplierFullName\":\"Катрен\",\"MNN\":\"ацетилсалициловая кислота\",\"Ost\":\"1\",\"UnitFullName\":\"упаковка\",\"ProducerShortName\":\"Нанофарм\",\"DrugFullName\":\"Аспирин-С\",\"OstLast\":\"1\",\"DrugsShortName\":\"Аспирин\",\"Packaging\":\"упаковки\",\"ID\":\"10001\",\"UnitShortName\":\"упак.\"}]";
      println (json);

      val mapValues = JsonUtil.fromJson[List[Map[String, String]]](json);
      val values = mapValues.map (v => {
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

      Ok("File uploaded")
    }.get

    Future {Ok("uploaded")}
  }
}
