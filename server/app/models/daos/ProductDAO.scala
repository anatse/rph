package models.daos

import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import models.DrugsProduct

import scala.concurrent.Future

trait ProductDAO extends BaseDAO[DrugsProduct] {
  def fuzzySearch (text: String, sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsProduct]]
  def textSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsProduct]]
  def combinedSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsProduct]]
  def findByGroup (group: Array[String], text: Option[String], sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsProduct]]



  // Admin functions
  def createTextIndex ():Future[WriteResult]
  def bulkInsert (entities: List[DrugsProduct]): Future[Unit]
  def bulkUpsert (entities: List[DrugsProduct]): Future[Seq[UpdateWriteResult]]

  // Product update functions
  def addImage (id: String, imageUrl: String):Future[Option[DrugsProduct]]
  def setGroups (id: String, groups: Array[String]):Future[Option[DrugsProduct]]

}
