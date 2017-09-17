package models.daos

import reactivemongo.api.commands.WriteResult
import models.DrugsProduct
import scala.concurrent.Future

trait ProductDAO extends BaseDAO[DrugsProduct] {
  def textSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsProduct]]
  def createTextIndex ():Future[WriteResult]
}
