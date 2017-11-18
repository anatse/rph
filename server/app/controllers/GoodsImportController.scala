package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.DrugsProduct
import models.daos.ProductDAO
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{JsonUtil, Logger}
import utils.auth.DefaultEnv

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class UpsertRes (ok:Int = 0, upserted: Int = 0, modified: Int = 0, errors: Int = 0)

class GoodsImportController @Inject()(
    components: ControllerComponents,
    silhouette: Silhouette[DefaultEnv],
    productDAO: ProductDAO)(implicit webJarsUtil: WebJarsUtil) extends AbstractController(components) with I18nSupport with Logger {

  implicit val resWrites = Json.writes[UpsertRes]
  implicit val resReads = Json.reads[UpsertRes]

  def upload = silhouette.SecuredAction (parse.multipartFormData).async { request =>
    val drugsToSave = Try[List[DrugsProduct]] (request.body.file("fileinfo").map { picture =>
      val filename = picture.filename
      val fileText = Source.fromFile(picture.ref.path.toString).mkString
      if (fileText.length == 0) throw (new RuntimeException(s"Empty file: $filename $fileText"))

      val mapValues = JsonUtil.fromJson[List[Map[String, Option[String]]]](fileText);
      mapValues.map (v => {
        DrugsProduct (
          retailPrice = v("RetailPrice").getOrElse("0").toDouble,
          barCode = v("BarCode").getOrElse(""),
          tradeTech = v("TradeTech").getOrElse(""),
          producerFullName = v("ProducerFullName").getOrElse(""),
          drugsFullName = v("DrugsFullName").getOrElse(""),
          supplierFullName = v("SupplierFullName").getOrElse(""),
          MNN = v("MNN").getOrElse(""),
          ost = v("Ost").getOrElse("0").toDouble,
          unitFullName = v("UnitFullName").getOrElse(""),
          producerShortName = v("ProducerShortName").getOrElse(""),
          drugsShortName = v("DrugsShortName").getOrElse(""),
          packaging = v("Packaging").getOrElse(""),
          id = v("ID").getOrElse(""),
          unitShortName = v("UnitShortName").getOrElse("")
        )
      })
    }.get)

    drugsToSave match {
      case Success(drugs) => productDAO.bulkUpsert(drugs).flatMap(
        results => {
          val obj = results.foldLeft(UpsertRes()){
            (obj, res) => {
              UpsertRes(
                ok = obj.ok + (if (res.ok) 1 else 0),
                upserted = obj.upserted + res.upserted.size,
                modified = obj.modified + res.nModified,
                errors = obj.errors + res.writeErrors.size
              )
            }
          }

          Future.successful(Ok(Json.obj("res" -> obj)))
        }
      )
      case Failure(e) => Future.successful(BadRequest(e.getMessage))
    }
  }
}
