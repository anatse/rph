package models.daos

import models.{DrugsGroup, DrugsProduct, MongoBaseDao}
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import javax.inject.Inject

import play.api.cache.{AsyncCacheApi, NamedCache, SyncCacheApi}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Text
import reactivemongo.bson.{BSONArray, BSONDocumentReader, BSONDocumentWriter, BSONElement, Macros, document}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ProductDAOImpl @Inject() (val mongoApi: ReactiveMongoApi, @NamedCache("user-cache")cacheApi: SyncCacheApi, implicit val ex: ExecutionContext) extends MongoBaseDao with ProductDAO {
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
  private def soundex (inpStr: String): String = cacheApi.getOrElseUpdate[String](s"soundex.$inpStr"){
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
  private def prepareDrug (drug: DrugsProduct): DrugsProduct = {
    val soundexWords = drug.drugsFullName.split(regexp).map(soundex(_)).foldLeft (drug.MNN.split(regexp).map(soundex(_))) (
      (arr, item) => {
        if (item == "" || arr.contains(item)) arr else arr :+ item
      }
    )

    drug.copy(sndWords = Some(soundexWords))
  }

  private def fixKeyboardLayout (str: String): String = cacheApi.getOrElseUpdate[String](s"ruslayout.$str"){
    val replacer = Map(
      'q' -> 'й', 'w' -> 'ц', 'e' -> 'у', 'r' -> 'к', 't' -> 'е', 'y' -> 'н', 'u' -> 'г',
      'i' -> 'ш', 'o' -> 'щ', 'p' -> 'з', '[' -> 'х', ']' -> 'ъ', 'a' -> 'ф', 's' -> 'ы',
      'd' -> 'в', 'f' -> 'а', 'g' -> 'п', 'h' -> 'р', 'j' -> 'о', 'k' -> 'л', 'l' -> 'д',
      ';' -> 'ж', '\'' -> 'э', 'z' -> 'я', 'x' -> 'ч', 'c' -> 'с', 'v' -> 'м', 'b' -> 'и',
      'n' -> 'т', 'm' -> 'ь', ',' -> 'б', '.' -> 'ю', '/' -> '.'
    )

    str.toLowerCase.map(c => replacer.getOrElse(c, c))
  }

  private lazy val projection = document (
    "barCode" -> 1,
    "id" -> 1,
    "drugsFullName" -> 1,
    "drugFullName" -> 1,
    "drugsShortName" -> 1,
    "ost" -> 1,
    "ostFirst" -> 1,
    "ostLast" -> 1,
    "retailPrice" -> 1,
    "MNN" -> 1,
    "tradeTech" -> 1,
    "producerFullName" -> 1,
    "producerShortName" -> 1,
    "supplierFullName" -> 1,
    "unitFullName" -> 1,
    "unitShortName" -> 1,
    "packaging" -> 1,
    "drugGroups" -> 1,
    "shortName" -> 1,
    "drugImage" -> 1,
    "seoTags" -> 1
  )

  /**
    * Function find drugs by drusFullName only but user regexp search
    * @param text String to search
    * @param sortField sorting field
    * @param offset offset
    * @param pageSize pageSize
    * @return list of the found drugs
    */
  def searchbyDrugName (text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(
    document (BSONElement.provided("$and" ->
      BSONArray(
        document("$or" -> BSONArray(
            document("drugsFullName" -> document("$regex" -> s".*${text}.*", "$options" -> "i")),
            document("drugsFullName" -> document("$regex" -> s".*${fixKeyboardLayout(text)}.*", "$options" -> "i"))
          )
        ),
        document("ost" -> document("$gt" -> 0))
      )))).options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(document(sortField.getOrElse("retailPrice") -> 1))
    .cursor[DrugsProduct]()
    .collect[List](pageSize, handler[DrugsProduct]))

  /**
    * Function searches drugs using mongo text index. This text index custructs from name and description and producer of drugs
    * @param text text to search
    * @param sortField field to sroting result set
    * @param offset offset
    * @param pageSize page size
    * @return list of found drugs
    */
  def textSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(
    document (BSONElement.provided("$and" ->
      BSONArray(
        document("$text" -> document(
          "$search" -> text,
          "$caseSensitive" -> false)
        ),
        document("ost" -> document("$gt" -> 0))
      )))).projection(projection).options(QueryOpts().skip(offset).batchSize(pageSize))
      .sort(document(sortField.getOrElse("retailPrice") -> 1))
      .cursor[DrugsProduct]()
      .collect[List](pageSize, handler[DrugsProduct]))

  /**
    * Function searchs drugs using soundex function. This function prepares array of words and stored its within drugs object.
    * Eache text string will be splitted by words and next these words converts to sondex form. After mongo searchs intersects between
    * thwo words arrays - stored in database and computed from search string
    * @param text text toi search
    * @param sortField sort field
    * @param offset offset
    * @param pageSize page size
    * @return list of found drugs
    */
  override def fuzzySearch(text: String, sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap (col => {
    val words = text.split(regexp).map(soundex(_))
    // Add fixed layout strings to find
    val allWords = words ++ fixKeyboardLayout(text).split(regexp).map(soundex(_))

    col.find(
      document (BSONElement.provided("$and" ->
        BSONArray(
          document("sndWords" -> document("$in" -> allWords)),
          document("ost" -> document("$gt" -> 0))
        ))
      )
    ).projection(projection).options(QueryOpts().skip(offset).batchSize(pageSize))
      .sort(document (sortField.getOrElse("retailPrice") -> 1))
      .cursor[DrugsProduct]()
      .collect[List](pageSize, handler[DrugsProduct])
  })

  override def findByGroup (group: Array[String], text: Option[String], sortField: Option[String], offset: Int, pageSize: Int): Future[List[DrugsProduct]] =
    productCollection.flatMap (col => {
      val arr = BSONArray(document("drugGroups" -> document("$in" -> group)))
      text match {
        case Some(txt) => val words = txt.split(regexp).map(soundex(_))
          val allWords = words ++ fixKeyboardLayout(txt).split(regexp).map(soundex(_))
          arr.add (document("sndWords" -> document("$in" -> allWords)))
        case _ =>
      }

      arr.add(document("ost" -> document("$gt" -> 0)))

      col.find(
        document ("$and" -> arr)
      ).projection(projection).options(QueryOpts().skip(offset).batchSize(pageSize))
        .sort(document (sortField.getOrElse("retailPrice") -> 1))
        .cursor[DrugsProduct]()
        .collect[List](pageSize, handler[DrugsProduct])
    })

  /**
    * Function retrieves all drugs from database using sorting and paging
    * @param sortField sort
    * @param offset offset
    * @param pageSize page size
    * @return list of found drugs
    */
  override def findAll(sortField: Option[String], offset: Int, pageSize: Int) = productCollection.flatMap(_.find(document ("ost" -> document("$gt" -> 0)))
    .options(QueryOpts().skip(offset).batchSize(pageSize))
    .sort(document(sortField.getOrElse("retailPrice") -> 1))
    .cursor[DrugsProduct]()
    .collect[List](pageSize, handler[DrugsProduct]))

  override def findById(id: String) = productCollection.flatMap(_.find(document("_id" -> id)).projection(projection).one[DrugsProduct])
  override def save(product: DrugsProduct) = productCollection.flatMap(_.update(document("_id" -> product.id), product, upsert = true).map(_.upserted.map(ups => product).head))
  override def remove(id: String) = productCollection.flatMap(_.remove(document("_id" -> id)).map(r => {}))

  /**
    * Function updates or inserts given products. Only part of the drug attributes wil be changes. Suck additional attribues as
    * groups, seo tags and so on not changes by bulk upserts. Its can be changed manually only
    * @param entities
    * @return list of status of the operatiions
    */
  override def bulkUpsert (entities: List[DrugsProduct]): Future[Seq[UpdateWriteResult]] = Future.sequence (
    entities.map(prepareDrug(_)).map (entity => {
        val res = productCollection.flatMap(_.update(
          document(
            "_id" -> entity.id),
            document (
              "$set" -> document (
                "_id" -> entity.id,
                "barCode" -> entity.barCode,
                "drugsFullName" -> entity.drugsFullName,
                "drugFullName" -> entity.drugFullName,
                "drugsShortName" -> entity.drugsShortName,
                "ost" -> entity.ost,
                "ostFirst" -> entity.ostFirst,
                "ostLast" -> entity.ostLast,
                "retailPrice" -> entity.retailPrice,
                "tradeTech" -> entity.tradeTech,
                "producerFullName" -> entity.producerFullName,
                "producerShortName" -> entity.producerShortName,
                "supplierFullName" -> entity.supplierFullName,
                "MNN" -> entity.MNN,
                "unitFullName" -> entity.unitFullName,
                "unitShortName" -> entity.unitShortName,
                "packaging" -> entity.packaging,
                "sndWords" -> entity.sndWords
              )
            ),
            upsert = true
          )
        )
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

  /**
    * Function combines all types of search drugs in the following order <br/>
    * Next search algorithm will be used only if the previous search found no results
    * <ul>
    *   <li>Find by name</li>
    *   <li>Text search</li>
    *   <li>Fuzzy search</li>
    * </ul>
    *
    * @param text test to search
    * @param sortField sorting
    * @param offset offset
    * @param pageSize page size
    * @return list of found drugs
    */
  override def combinedSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int) = searchbyDrugName(text, sortField, offset, pageSize).flatMap(
    res =>
      // If not found then trying to text search
      if (res.size == 0)
        textSearch(text, sortField, offset, pageSize).flatMap(
          result =>
            // If not found then trying to fuzzy search
            if (result.size == 0)
              fuzzySearch (text, sortField, offset, pageSize)
            else
              Future.successful(result)
        )
      else
        Future.successful(res)
  )

  /**
    * Not used because andThen not working as I imagined.
    * TODO fix my imagination
    * @param text
    * @param sortField
    * @param offset
    * @param pageSize
    * @return
    */
  @Deprecated
  def _notWorkingCombinedSearch (text: String, sortField: Option[String], offset: Int, pageSize: Int) =
    searchbyDrugName(text, sortField, offset, pageSize) andThen {
      case Success(res) if (res.size == 0) =>
        textSearch(text, sortField, offset, pageSize) andThen {
          case Success(result) if (result.size == 0) => fuzzySearch (text, sortField, offset, pageSize)
        }
    }

  /**
    * Function set image to desired drug
    * @param id
    * @param imageUrl
    * @return changed drug
    */
  override def addImage(id: String, imageUrl: String) = productCollection.flatMap(_.findAndUpdate(
      document("_id" -> id),
      document("$set" -> document ( "drugImage" -> imageUrl)),
      fetchNewObject = true
    ).map(r => r.result[DrugsProduct]))

  /**
    * Function set image to desired drug
    * @param id
    * @param groups
    * @return changed drug
    */
  override def setGroups (id: String, groups: Array[String]) = productCollection.flatMap(_.findAndUpdate(
      document("_id" -> id),
      document("$set" -> document ( "drugGroups" -> groups)),
      fetchNewObject = true
    ).map(r => r.result[DrugsProduct]))
}
