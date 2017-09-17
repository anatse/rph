package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import model.ProjectDAO
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
    silhouette: Silhouette[DefaultEnv],
    pdao: ProjectDAO)(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

  case class Good (
    RetailPrice: String,
    BarCode: String,
    OstFirst: String,
    TradeTech: String,
    ProducerFullName: String,
    DrugsFullName: String,
    SupplierFullName: String,
    MNN: String,
    Ost: String,
    UnitFullName: String,
    ProducerShortName: String,
    DrugFullName: String,
    OstLast: String,
    DrugsShortName: String,
    Packaging: String,
    ID: String,
    UnitShortName: String
  )

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
        Good (
          RetailPrice = v("RetailPrice"),
          BarCode = v("BarCode"),
          OstFirst = v("OstFirst"),
          TradeTech = v("TradeTech"),
          ProducerFullName = v("ProducerFullName"),
          DrugsFullName = v("DrugsFullName"),
          SupplierFullName = v("SupplierFullName"),
          MNN = v("MNN"),
          Ost = v("Ost"),
          UnitFullName = v("UnitFullName"),
          ProducerShortName = v("ProducerShortName"),
          DrugFullName = v("DrugFullName"),
          OstLast = v("OstLast"),
          DrugsShortName = v("DrugsShortName"),
          Packaging = v("Packaging"),
          ID = v("ID"),
          UnitShortName = v("UnitShortName")
        )
      })

      println (values)

      Ok("File uploaded")
    }.get

    Future {Ok("uploaded")}
  }
}
