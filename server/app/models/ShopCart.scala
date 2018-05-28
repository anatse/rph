package models

case class ShopCart (userId: Option[String], sessionId: String, items: Array[ShopCartItem])
case class ShopCartItem (drugId: String, drugName: String, num: Int, price: Double)

case class RemoteCartItem (drugId: String, drugName: String, num: Int, price: Double, availableOnStock: Int, producer: String)
case class RemoteCart (
  userUuid: String,
  userName: String,
  userPhone: String,
  userMail: String,
  comment: String,
  items: List[RemoteCartItem]
)