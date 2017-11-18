package models.daos

import models.{ShopCart, ShopCartItem}

import scala.concurrent.Future

trait CartDAO extends BaseDAO[ShopCart] {
  def find (cart: ShopCart): Future[Option[ShopCart]]
  def saveItem(cart: ShopCart, shopCartItem: ShopCartItem): Future[Option[ShopCart]]
  def removeItem(cart: ShopCart, shopCartItem: ShopCartItem): Future[Unit]
}
