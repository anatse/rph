package models.daos

import models.DrugsGroup
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

trait DrugsGroupDAO extends BaseDAO[DrugsGroup] {
  def textSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsGroup]]
  def createTextIndex ():Future[WriteResult]
}
