package models.mongo

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Text

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.api.{Cursor, DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

// Information about mongodb module http://reactivemongo.org/releases/0.12/documentation/tutorial/play.html

// Custom persistent types
case class Person(firstName: String, lastName: String, age: Int)

class MongoDao {
  val mongoUri = "mongodb://localhost:27017/shopdb?authMode=scram-sha1"
  import ExecutionContext.Implicits.global // use any appropriate context
  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def db1: Future[DefaultDB] = futureConnection.flatMap(_.database("shopdb"))
  def personCollection:Future[BSONCollection] = db1.map(_.collection("person"))

  // Write Documents: insert or update

  implicit def personWriter: BSONDocumentWriter[Person] = Macros.writer[Person]
  // or provide a custom one

  def createTextIndex ():Future[WriteResult] = {
    personCollection.flatMap(
      pc => pc.indexesManager.create(Index(
        key = Seq(
          "lastName" -> Text,
          "firstName" -> Text
        ),
        name = Some("personNameText"),
        options = document (
          "default_language" -> "russian"
        )
      ))
    )
  }

  /**
    * Searches using mongo full text search engine
    * @param lastName
    * @see https://docs.mongodb.com/v3.2/reference/operator/query/text/#text-query-operator-behavior
    * @return
    */
  def findByText (lastName: String): Future[List[Person]] = {
    personCollection.flatMap(_.find(document("$text" -> document (
      "$search" -> lastName,
      "$caseSensitive" -> false)
    )).cursor[Person]().collect[List](-1, handler))
  }

  def createPerson(person: Person): Future[Unit] = personCollection.flatMap(
    pc => {
      println (pc)
      pc.insert(person).map(_ => {})
    }
  ) // use personWriter

  def updatePerson(person: Person): Future[Int] = {
    val selector = document(
      "firstName" -> person.firstName,
      "lastName" -> person.lastName
    )

    // Update the matching person
    personCollection.flatMap(_.update(selector, person).map(_.n))
  }

  implicit def personReader: BSONDocumentReader[Person] = Macros.reader[Person]
  // or provide a custom one

  val handler: Cursor.ErrorHandler[List[Person]] = { (last: List[Person], error: Throwable) =>
    println(s"Encounter error: $error")

    if (last.isEmpty) { // continue, skip error if no previous value
      Cursor.Cont(last)
    }
    else Cursor.Fail(error)
  }

  def findPersonByAge(age: Int): Future[List[Person]] =
    personCollection.flatMap(_.find(document("age" -> age)). // query builder
      cursor[Person]().collect[List](-1, handler)) // collect using the result cursor
}
