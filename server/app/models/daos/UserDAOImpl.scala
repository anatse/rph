package models.daos

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (@NamedDatabase("estima") protected val dbConfigProvider: DatabaseConfigProvider, implicit val ex: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] with UserDAO {
  import profile.api._

  implicit class ResultEnrichment[T](action: DBIO[Seq[T]]) {
    def exactlyOne: DBIO[Option[T]] = action.flatMap { xs =>
      xs.length match {
        case 1 => DBIO.successful(Some(xs.head))
        case 0 => DBIO.successful(None)
        case n => DBIO.failed(new RuntimeException(s"Expected 1 result, not $n"))
      }
    }
  }

  class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def firstName = column[Option[String]]("FIRST_NAME")
    def lastName = column[Option[String]]("LAST_NAME")
    def fullName = column[Option[String]]("FULL_NAME")
    def email = column[Option[String]]("EMAIL")
    def avatarURL = column[Option[String]]("AVATAR_URL")
    def providerID = column[Option[String]]("PROVIDER_ID")
    def providerKey = column[Option[String]]("PROVIDER_KEY")
    def activated = column[Boolean]("ACTIVATED")
    def userID = column[UUID]("EXT_USER_ID", O.PrimaryKey)

    def * = (userID, providerID, providerKey, firstName, lastName, fullName, email, avatarURL, activated) <> (User.tupled, User.unapply _)
  }

  // Create table
  val users = TableQuery[UserTable]

  def createTable () = {
    if (!checkTableExists()) {
      println ("start creation table")
      Await.result(db.run(DBIO.seq(users.schema.create)), 1 second)
    }
  }

  def checkTableExists ():Boolean = {
    val tables = Await.result(db.run(getTables), 1 seconds)
    val isExists = !tables.filter(t => t.name.name.contains("users")).isEmpty
    isExists
  }

  createTable

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    val query = users.filter(u => u.providerID === loginInfo.providerID && u.providerKey === loginInfo.providerKey)
    db.run(query.result.exactlyOne)
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = {
    val query = users.filter(u => u.userID === userID)
    db.run(query.result.exactlyOne)
  }

//  def findAll = {
//    val list = Await.result(db.run(users.result), 1 second)
//    list.foreach(
//      u => println(s"user: $u")
//    )
//  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    val userWithId = (users returning users).insertOrUpdate (user).map (u => u)
    db.run(userWithId.map(u => u.getOrElse(user)))
  }
}

