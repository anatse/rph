package models.daos

import models.DrugsProduct
import models.MongoBaseDao
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import javax.inject.Inject

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.QueryOpts
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Text
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.ExecutionContext

class ProductDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, implicit val ex: ExecutionContext) extends MongoBaseDao with ProductDAO {
  private def productCollection:Future[BSONCollection] = mongoApi.database.map(_.collection("products"))

  implicit def productWriter: BSONDocumentWriter[DrugsProduct] = Macros.writer[DrugsProduct]
  implicit def productReader: BSONDocumentReader[DrugsProduct] = Macros.reader[DrugsProduct]

  def createTextIndex () = productCollection.flatMap(
    collection => collection.indexesManager.create(Index(
      key = Seq(
        "drugsFullName" -> Text,
        "drugsShortName" -> Text,
        "drugFullName" -> Text,
        "producerFullName" -> Text
      ),
      name = Some("productSearchText"),
      options = document (
        "default_language" -> "russian"
      )
    ))
  )

  def textSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(
      document("$text" -> document (
        "$search" -> text,
        "$caseSensitive" -> false)
    )).options(QueryOpts().skip(offset).batchSize(pageSize))
      .sort(sortField match {
        case Some(value:String) => BSONDocument(value -> 1)
        case _ => BSONDocument.empty
      })
      .cursor[DrugsProduct]()
      .collect[List](-1, handler[DrugsProduct]))

  override def findAll(sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(document())
    .options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(sortField match {
      case Some(value:String) => BSONDocument(value -> 1)
      case _ => BSONDocument.empty
    })
    .cursor[DrugsProduct]()
    .collect[List](-1, handler[DrugsProduct]))

  override def findById(id: String) = productCollection.flatMap(_.find(document("id" -> id)).one[DrugsProduct])

  override def save(product: DrugsProduct) = productCollection.flatMap(_.update(document("id" -> product.id), product, upsert = true).map(_.upserted.map(ups => product).head))

  override def remove(id: String) = productCollection.flatMap(_.remove(document("id" -> id)).map(r => {}))
}
