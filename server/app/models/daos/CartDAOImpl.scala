package models.daos

import javax.inject.Inject

import models.{MongoBaseDao, ShopCart, ShopCartItem}
import play.api.cache.AsyncCacheApi
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

class CartDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, cacheApi: AsyncCacheApi, implicit val ex: ExecutionContext) extends MongoBaseDao with CartDAO {
  private def cartCollection: Future[BSONCollection] = mongoApi.database.map(_.collection("cart"))

  implicit def cartItemWriter: BSONDocumentWriter[ShopCartItem] = Macros.writer[ShopCartItem]
  implicit def cartItemReader: BSONDocumentReader[ShopCartItem] = Macros.reader[ShopCartItem]
  implicit def cartWriter: BSONDocumentWriter[ShopCart] = Macros.writer[ShopCart]
  implicit def cartReader: BSONDocumentReader[ShopCart] = Macros.reader[ShopCart]

  override def findById(id: String) = ???

  def find(cart: ShopCart) = /*cacheApi.getOrElseUpdate[Option[ShopCart]](cart.userId.getOrElse(cart.sessionId))*/ {
    cartCollection.flatMap(_.find(
      if (!cart.userId.isEmpty) {
        document("$or" -> BSONArray(
          document("userId" -> cart.userId.get),
          document("sessionId" -> cart.sessionId)
        ))
      } else {
        document("sessionId" -> cart.sessionId)
      }).one[ShopCart])
  }

  def findOrInsert(cart: ShopCart): Future[ShopCart] = /*cacheApi.getOrElseUpdate[ShopCart](cart.userId.getOrElse(cart.sessionId))*/ {
    find(cart).flatMap(
      res => res match {
        case Some(c) => if (c.userId != cart.userId && !cart.userId.isEmpty)
          save(cart.copy(userId = cart.userId))
        else
          Future.successful(c)

        case None => save(cart)
      }
    )
  }

  override def save(entity: ShopCart) = cartCollection.flatMap(
    _.update(
      selector = document (
        if (!entity.userId.isEmpty)
          document("userId" -> entity.userId)
        else
          document("sessionId" -> entity.sessionId)
      ),
      update = entity,
      upsert = true
    ).map(res =>
      if (res.ok) {
        entity
      }
      else null
    )
  )

  override def remove(id: String) = cartCollection.flatMap(
    _.remove(document("$or" -> BSONArray(
        document("userId" -> id),
        document("sessionId" -> id)
      )
    )).map(_ => {})
  )

  def updateItems (newCart:ShopCart, items: Array[ShopCartItem]):Future[Option[ShopCart]] = cartCollection.flatMap(_.findAndUpdate(
    document (
      if (!newCart.userId.isEmpty)
        document("userId" -> newCart.userId)
      else
        document("sessionId" -> newCart.sessionId)
    ),
    document("$set" -> document ( "items" -> items)),
    fetchNewObject = true
  ).map(r => r.result[ShopCart]))

  override def saveItem(cart: ShopCart, shopCartItem: ShopCartItem) = //cacheApi.remove(cart.userId.getOrElse(cart.sessionId)).flatMap(_ =>
    findOrInsert(cart).flatMap(
    res => {
      val found = res.items.find(_.drugId == shopCartItem.drugId)
      if (found.isEmpty) {
        val items = res.items.filter(_.drugId != shopCartItem.drugId)
        val newCart = res.copy(items = items :+ shopCartItem)
        updateItems(res, newCart.items)
      } else {
        if (found.get.num != shopCartItem.num) {
          val items = res.items.filter(_.drugId != shopCartItem.drugId)
          if (shopCartItem.num > 0 ) {
            var ni = res.items.map(r => if (r.drugId == shopCartItem.drugId) r.copy(num = shopCartItem.num) else r)
            val newCart = res.copy(items = ni)
            updateItems(res, newCart.items)
          } else {
            val newCart = res.copy(items = items)
            updateItems(res, newCart.items)
          }
        }
        else
          Future.successful(Some(res))
      }
    }
  )

  override def removeItem(cart: ShopCart, shopCartItem: ShopCartItem) = //cacheApi.remove(cart.userId.getOrElse(cart.sessionId)).flatMap(_ =>
   findOrInsert(cart).flatMap(
    res => {
      val items = res.items.filter(_.drugId != shopCartItem.drugId)
      updateItems(res, items)
    }
  ).map(r => {})

  override def findAll(sortField: Option[String], offset: Int, pageSize: Int) = ???
}