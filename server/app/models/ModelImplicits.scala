package models

import play.api.libs.json.Json

trait ModelImplicits {
  import play.api.mvc.Results._

  implicit val productWrites = Json.writes[DrugsProduct]
  implicit val productReads = Json.reads[DrugsProduct]

  implicit val producRsWrites = Json.writes[DrugsProdRs]
  implicit val producRsReads = Json.reads[DrugsProdRs]

  implicit val productRqWrites = Json.writes[DrugsFindRq]
  implicit val drugsFindRqReads = Json.reads[DrugsFindRq]

  implicit val groupWrites = Json.writes[DrugsGroup]
  implicit val groupReads = Json.reads[DrugsGroup]

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
}
