package models

case class ShopCart (userId: String, items: Array[ShopCartItem])
case class ShopCartItem (drugId: String, drugName: String, num: Int, price: Double)
