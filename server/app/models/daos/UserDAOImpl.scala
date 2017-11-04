package models.daos

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.{MongoBaseDao, User}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros, document}
import utils.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (val mongoApi: ReactiveMongoApi)(implicit val ex: ExecutionContext) extends UserDAO with MongoBaseDao with Logger {
  /**
    * Create users table/collection
    * @return users mongodb collections
    */
  private def usersCollection:Future[BSONCollection] = mongoApi.database.map(_.collection("users"))

  implicit def userWriter: BSONDocumentWriter[User] = Macros.writer[User]
  implicit def userReader: BSONDocumentReader[User] = Macros.reader[User]

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    usersCollection.flatMap(_.find(
      document(
        "providerID" -> loginInfo.providerID,
        "providerKey" -> loginInfo.providerKey
      )
    ).one[User])
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = usersCollection.flatMap(_.find(document("userID" -> userID)).one[User])

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    // Insert or update the user
    usersCollection.flatMap(_.update(document("userID" -> user.userID), user, upsert = true).map(_.upserted.map(ups => user) .headOption.getOrElse(user)))
  }
}

