package models

case class DrugsFindRq (groups: Option[Array[String]], text: Option[String], sorts: Option[String], offset: Int, pageSize: Int, drugImage: Option[String])
case class DrugsProdRs (dp: DrugsProduct, countInCart: Int)