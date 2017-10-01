package models.daos

import models.{DrugsGroup, DrugsProduct, MongoBaseDao}
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import javax.inject.Inject

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Text
import reactivemongo.bson.{BSONArray, BSONDocumentReader, BSONDocumentWriter, Macros, document}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ProductDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, implicit val ex: ExecutionContext) extends MongoBaseDao with ProductDAO {
  private def productCollection:Future[BSONCollection] = mongoApi.database.map(_.collection("products"))
  private val regexp = "[ ,.\\+-]+"

  implicit def productWriter: BSONDocumentWriter[DrugsProduct] = Macros.writer[DrugsProduct]
  implicit def productReader: BSONDocumentReader[DrugsProduct] = Macros.reader[DrugsProduct]

  def createTextIndex () = productCollection.flatMap(
    collection => collection.indexesManager.create(Index(
      key = Seq(
        "drugsFullName" -> Text,
        "drugsShortName" -> Text,
        "drugFullName" -> Text,
        "producerFullName" -> Text
      ),
      name = Some("productSearchText"),
      options = document (
        "default_language" -> "russian"
      )
    ))
  )

  /**
    * Function used to create soundex representation for the given word
    * @param inpStr
    * @return soundex representation
    */
  def soundex (inpStr: String): String = {
    List[(String, String)](
      ("ЙО|ИО|ЙЕ|ИЕ" -> "И"),
      ("О|Ы|Я" -> "А"),
      ("Е|Ё|Э" -> "И"),
      ("Ю" -> "У"),
      ("Б" -> "П"),
      ("З" -> "С"),
      ("Д" -> "Т"),
      ("В" -> "Ф"),
      ("Г" -> "К"),
      ("ТС|ДС" -> "Ц"),
      ("Н{2,}" -> "Н"),
      ("С{2,}" -> "С"),
      ("Р{2,}" -> "Р"),
      ("М{2,}" -> "М"),
      ("[УЕАЫОИЯЮЭ]{1,}$" -> "")
    ).foldLeft(inpStr.toUpperCase())((inp, m) => {inp.replaceAll(m._1, m._2)})
  }

  /**
    * Fills sondex words representation for given drug
    * @param drug
    * @return new drug product with soundex words
    */
  def prepareDrug (drug: DrugsProduct): DrugsProduct = {
    val soundexWords = drug.drugsFullName.split(regexp).map(soundex(_)).foldLeft (drug.MNN.split(regexp).map(soundex(_))) (
      (arr, item) => {
        if (item == "" || arr.contains(item)) arr else arr :+ item
      }
    )

    drug.copy(sndWords = Some(soundexWords))
  }

  def fixKeyboardLayout (str: String): String = {
    val replacer = Map(
      'q' -> 'й', 'w' -> 'ц', 'e' -> 'у', 'r' -> 'к', 't' -> 'е', 'y' -> 'н', 'u' -> 'г',
      'i' -> 'ш', 'o' -> 'щ', 'p' -> 'з', '[' -> 'х', ']' -> 'ъ', 'a' -> 'ф', 's' -> 'ы',
      'd' -> 'в', 'f' -> 'а', 'g' -> 'п', 'h' -> 'р', 'j' -> 'о', 'k' -> 'л', 'l' -> 'д',
      ';' -> 'ж', '\'' -> 'э', 'z' -> 'я', 'x' -> 'ч', 'c' -> 'с', 'v' -> 'м', 'b' -> 'и',
      'n' -> 'т', 'm' -> 'ь', ',' -> 'б', '.' -> 'ю', '/' -> '.'
    )

    str.toLowerCase.map(c => replacer.getOrElse(c, c))
  }

  def textSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(
      document("$text" -> document (
        "$search" -> text,
        "$caseSensitive" -> false)
    )).options(QueryOpts().skip(offset).batchSize(pageSize))
      .sort(document(sortField.getOrElse("retailPrice") -> 1))
      .cursor[DrugsProduct]()
      .collect[List](pageSize, handler[DrugsProduct]))

  override def findAll(sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(document())
    .options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(document(sortField.getOrElse("retailPrice") -> 1))
    .cursor[DrugsProduct]()
    .collect[List](pageSize, handler[DrugsProduct]))

  override def findById(id: String) = productCollection.flatMap(_.find(document("_id" -> id)).one[DrugsProduct])
  override def save(product: DrugsProduct) = productCollection.flatMap(_.update(document("_id" -> product.id), product, upsert = true).map(_.upserted.map(ups => product).head))
  override def remove(id: String) = productCollection.flatMap(_.remove(document("_id" -> id)).map(r => {}))

  override def bulkUpsert (entities: List[DrugsProduct]): Future[Seq[UpdateWriteResult]] = Future.sequence (
    entities.map(prepareDrug(_)).map (entity => {
        val res = productCollection.flatMap(_.update(document("_id" -> entity.id), entity, upsert = true))
        res.onComplete {
          case Failure(e) => e.printStackTrace()
          case Success(writeResult) => {}
        }

        res
      }
    )
  )

  override def bulkInsert(entities: List[DrugsProduct]): Future[Unit] = productCollection.flatMap(
      col => {
        val bulkDocs = entities.map(implicitly[col.ImplicitlyDocumentProducer](_))
        col.bulkInsert(ordered = true)(bulkDocs:_*)
      }
    ).map(_ => {})

  override def fuzzySearch(text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap (col => {
    val words = text.split(regexp).map(soundex(_))
    // Add fixed layout strings to find
    val allWords = words ++ fixKeyboardLayout(text).split(regexp).map(soundex(_))

    col.find(
      document ("sndWords" -> document("$in" -> allWords))
    ).options(QueryOpts().skip(offset).batchSize(pageSize))
      .sort(document (sortField.getOrElse("retailPrice") -> 1))
      .cursor[DrugsProduct]()
      .collect[List](pageSize, handler[DrugsProduct])
  })

  def fuzzySearchOld(text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(
    document ("$where" -> s"compareString (this.drugsFullName, '${text}')"))
    .options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(document (sortField.getOrElse("retailPrice") -> 1))
    .cursor[DrugsProduct]()
    .collect[List](pageSize, handler[DrugsProduct]))

  override def combinedSearch(text: String, sortField: Option[String], offset: Int, pageSize: Int) = textSearch(text, sortField, offset, pageSize).flatMap(
    result =>
      // If not found then trying to fuzzy search
      if (result.size == 0)
        fuzzySearch (text, sortField, offset, pageSize)
      else
        Future(result)
  )
}
