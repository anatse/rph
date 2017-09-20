package models.daos
import javax.inject.Inject

import models.{DrugsGroup, MongoBaseDao}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.QueryOpts
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Text
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

class DrugsGroupDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, implicit val ex: ExecutionContext) extends MongoBaseDao with DrugsGroupDAO {
  private def groupCollection:Future[BSONCollection] = mongoApi.database.map(_.collection("drugsgroup"))

  implicit def drugsGroupWriter: BSONDocumentWriter[DrugsGroup] = Macros.writer[DrugsGroup]
  implicit def drugsGroupReader: BSONDocumentReader[DrugsGroup] = Macros.reader[DrugsGroup]

  def createTextIndex () = groupCollection.flatMap(
    collection => collection.indexesManager.create(Index(
      key = Seq(
        "groupName" -> Text,
        "description" -> Text
      ),
      name = Some("drugsGroupSearchText"),
      options = document (
        "default_language" -> "russian"
      )
    ))
  )

  override def textSearch(text: String, sortField: Option[String], offset: Int, pageSize: Int) = groupCollection.flatMap(_.find(
    document("$text" -> document (
      "$search" -> text,
      "$caseSensitive" -> false)
    )).options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(sortField match {
      case Some(value:String) => BSONDocument(value -> 1)
      case _ => BSONDocument.empty
    })
    .cursor[DrugsGroup]()
    .collect[List](-1, handler[DrugsGroup]))

  override def findAll(sortField: Option[String], offset: Int, pageSize: Int) = groupCollection.flatMap(_.find(document())
    .options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(sortField match {
      case Some(value:String) => BSONDocument(value -> 1)
      case _ => BSONDocument.empty
    })
    .cursor[DrugsGroup]()
    .collect[List](-1, handler[DrugsGroup]))

  override def findById(id: String) = groupCollection.flatMap(_.find(document("id" -> id)).one[DrugsGroup])
  override def save(group: DrugsGroup) = groupCollection.flatMap(_.update(document("id" -> group.id), group, upsert = true).map(_.upserted.map(ups => group).head))
  override def remove(id: String) = groupCollection.flatMap(_.remove(document("id" -> id)).map(r => {}))
}
