package models.daos

import java.util.UUID
import javax.inject.Inject

import models.AuthToken
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._



/**
 * Give access to the [[AuthToken]] object.
 */
class AuthTokenDAOImpl @Inject() (@NamedDatabase("estima") protected val dbConfigProvider: DatabaseConfigProvider, implicit val ex: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] with AuthTokenDAO {
  import profile.api._
  import com.github.tototoshi.slick.PostgresJodaSupport._

  implicit class ResultEnrichment[T](action: DBIO[Seq[T]]) {
    def exactlyOne: DBIO[Option[T]] = action.flatMap { xs =>
      xs.length match {
        case 1 => DBIO.successful(Some(xs.head))
        case 0 => DBIO.successful(None)
        case n => DBIO.failed(new RuntimeException(s"Expected 1 result, not $n"))
      }
    }
  }

  class AuthTokenTable(tag: Tag) extends Table[AuthToken](tag, "tokens") {
    def expiry = column[DateTime]("EXPIRY")
    def userID = column[UUID]("EXT_USER_ID")
    def id = column[UUID]("ID", O.PrimaryKey)
    def * = (id, userID, expiry) <> (AuthToken.tupled, AuthToken.unapply _)
  }

  // Create table
  val tokens = TableQuery[AuthTokenTable]
  def createTable () = {
    if (!checkTableExists())
      Await.result(db.run(DBIO.seq(tokens.schema.create)), 1 second)
  }

  def checkTableExists ():Boolean = {
    val tables = Await.result(db.run(getTables), 1 seconds)
    val isExists = !tables.filter(t => t.name.name.contains("tokens")).isEmpty
    isExists
  }

  createTable

  /**
   * Finds a token by its ID.
   *
   * @param id The unique token ID.
   * @return The found token or None if no token for the given ID could be found.
   */
  def find(id: UUID) = {
    var query = tokens.filter(u => u.id === id)
    val found = Await.result(db.run(query.result.exactlyOne), 1 second)
    Future.successful(found)
  }

  /**
   * Finds expired tokens.
   *
   * @param dateTime The current date time.
   */
  def findExpired(dateTime: DateTime) = {
    var query = tokens.filter(u => u.expiry <= dateTime)
    db.run(query.result)
  }

  /**
   * Saves a token.
   *
   * @param token The token to save.
   * @return The saved token.
   */
  def save(token: AuthToken) = {
    val tokenWithId = (tokens returning tokens.map(_.id) into ((token, id) => token.copy(id = id))) += token
    db.run(tokenWithId)
  }

  /**
   * Removes the token for the given ID.
   *
   * @param id The ID for which the token should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(id: UUID) = {
    var query = tokens.filter(u => u.id === id)
    db.run(query.delete).map(_ => ())
  }
}
