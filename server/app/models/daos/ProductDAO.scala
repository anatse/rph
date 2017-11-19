package models.daos

import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import models.{DrugsAdminRq, DrugsFindRq, DrugsProduct, RecommendedDrugs}

import scala.concurrent.Future

trait ProductDAO extends BaseDAO[DrugsProduct] {
  def getAll(dp:DrugsAdminRq): Future[List[DrugsProduct]]

  def combinedSearch (filter:DrugsFindRq): Future[List[DrugsProduct]]

  // Admin functions
  def createTextIndex ():Future[WriteResult]
  def bulkInsert (entities: List[DrugsProduct]): Future[Unit]
  def bulkUpsert (entities: List[DrugsProduct]): Future[Seq[UpdateWriteResult]]

  // Product update functions
  def addImage (id: String, imageUrl: String):Future[Option[DrugsProduct]]
  def setGroups (id: String, groups: Array[String]):Future[Option[DrugsProduct]]

  def findRecommended (offset: Int, pageSize: Int): Future[List[RecommendedDrugs]]
  def addRecommended (drugId: String, orderNum: Int): Future[Unit]
  def removeRecommended (drugId: String): Future[Unit]
}
