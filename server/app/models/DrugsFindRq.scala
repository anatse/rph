package models

case class DrugsProdRs (dp: DrugsProduct, countInCart: Int)

case class DrugsFindRq (
  groups: Option[Array[String]] = None,
  text: Option[String] = None,
  sorts: Option[Array[String]] = None,
  hasImage: Int,
  offset: Int,
  pageSize: Int
)