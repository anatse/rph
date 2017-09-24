package models

/**
  * Class stores information about product. Uses to transfer data from "m-apteka plus" database
  * @param id
  * @param barCode
  * @param drugsFullName
  * @param drugFullName
  * @param drugsShortName
  * @param ost
  * @param ostFirst
  * @param ostLast
  * @param retailPrice
  * @param tradeTech
  * @param producerFullName
  * @param producerShortName
  * @param supplierFullName
  * @param MNN
  * @param unitFullName
  * @param unitShortName
  * @param packaging
  */
case class DrugsProduct(
   id: String,
   barCode: String,

   drugsFullName: String,
   drugFullName: String,
   drugsShortName: String,

   ost: Double,
   ostFirst: Double,
   ostLast: Double,
   retailPrice: Double,

   tradeTech: String,
   producerFullName: String,
   producerShortName: String,
   supplierFullName: String,
   MNN: String,

   unitFullName: String,
   unitShortName: String,

   packaging: String,

   drugGroups: Option[Array[String]] = None,
   drugImage: Option[String] = None
)

case class DrugsGroup (id: String, groupName: String, description: String)
