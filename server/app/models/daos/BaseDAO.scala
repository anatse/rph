package models.daos

import models.MongoBaseDao
import scala.concurrent.Future

trait BaseDAO[T] extends MongoBaseDao {
  def findAll(sortField: Option[String], offset: Int, pageSize: Int): Future[List[T]]
  def findById (id: String): Future[Option[T]]
  def save (entity: T): Future[T]
  def remove(id: String): Future[Unit]
}
