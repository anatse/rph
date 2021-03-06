package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.MongoBaseDao
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros, document}
import utils.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * Class used to manipulate password information. All password information stored separately from user information
  * @param mongoApi mongo api
  * @param ex execution context
  */
class MongoAuthInfoDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, implicit val ex: ExecutionContext)
  extends DelegableAuthInfoDAO[PasswordInfo] with MongoBaseDao with Logger  {

  /**
    * Collection to stored password info - pwdinfo
    */
  private def pwdInfoCollection:Future[BSONCollection] = mongoApi.database.map(_.collection("pwdinfo"))

  implicit def pwdWriter: BSONDocumentWriter[PasswordInfo] = Macros.writer[PasswordInfo]
  implicit def pwdReader: BSONDocumentReader[PasswordInfo] = Macros.reader[PasswordInfo]

  override def find(loginInfo: LoginInfo) = pwdInfoCollection.flatMap(_.find(
    selector = document(
      "providerID" -> loginInfo.providerID,
      "providerKey" -> loginInfo.providerKey
    ),
    projection = document("hasher" -> 1, "password" -> 2, "salt" -> 3)
  ).one[PasswordInfo])

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo) = pwdInfoCollection.flatMap(_.insert(
    document(
      "providerID" -> loginInfo.providerID,
      "providerKey" -> loginInfo.providerKey,
      "hasher" -> authInfo.hasher,
      "password" -> authInfo.password,
      "salt" -> authInfo.salt
  ))).map(_ => authInfo)

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo) = save(loginInfo, authInfo)

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo) = pwdInfoCollection.flatMap(_.update(
    document(
      "providerID" -> loginInfo.providerID,
      "providerKey" -> loginInfo.providerKey),
    document(
      "providerID" -> loginInfo.providerID,
      "providerKey" -> loginInfo.providerKey,
      "hasher" -> authInfo.hasher,
      "password" -> authInfo.password,
      "salt" -> authInfo.salt),
    upsert = true).map(_ => authInfo))

  override def remove(loginInfo: LoginInfo) = pwdInfoCollection.flatMap(_.remove(document("providerID" -> loginInfo.providerID, "providerKey" -> loginInfo.providerKey)).map(r => {}))
}
