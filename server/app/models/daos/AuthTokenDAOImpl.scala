package models.daos

import java.util.UUID
import javax.inject.Inject

import models.{AuthToken, MongoBaseDao}
import org.joda.time.DateTime
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Give access to the [[AuthToken]] object.
 */
class AuthTokenDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, implicit val ex: ExecutionContext) extends AuthTokenDAO with MongoBaseDao {

  private def tokensCollection:Future[BSONCollection] = mongoApi.database.map(_.collection("tokens"))

  implicit def tokenWriter: BSONDocumentWriter[AuthToken] = Macros.writer[AuthToken]
  implicit def tokenReader: BSONDocumentReader[AuthToken] = Macros.reader[AuthToken]

  /**
   * Finds a token by its ID.
   *
   * @param id The unique token ID.
   * @return The found token or None if no token for the given ID could be found.
   */
  def find(id: UUID) = tokensCollection.flatMap(_.find(document("id" -> id)).one[AuthToken])

  /**
   * Finds expired tokens.
   *
   * @param dateTime The current date time.
   */
  def findExpired(dateTime: DateTime) = tokensCollection.flatMap(_.find(
      document(
        "expiry" -> document ("$lte" -> dateTime)
      )
    ).cursor[AuthToken]().collect[List](-1, handler[AuthToken]))

  /**
   * Saves a token.
   *
   * @param token The token to save.
   * @return The saved token.
   */
  def save(token: AuthToken) = tokensCollection.flatMap(_.insert(token).map(ins => token))

  /**
   * Removes the token for the given ID.
   *
   * @param id The ID for which the token should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(id: UUID) = tokensCollection.flatMap(_.remove(document("id" -> id)).map(r => {}))
}
