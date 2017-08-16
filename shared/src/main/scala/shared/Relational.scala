package shared

// http://kladr-api.ru/examples
case class Address (region: String, district: Option[String] = None, city: String, street: String, building: String, zipCode: Option[String] = None, kladrCode: Option[String] = None)
case class Client (
  firstName: String,
  lastName: Option[String] = None,
  middleName: Option[String] = None,
  email: String,
  phone: Seq[String] = Seq.empty[String],
  address: Seq[Address] = Seq.empty[Address]
)

// Additional attribute definition of the product
case class Attribute (
  name: String,
  description: Option[String] = None
)

// Parameters of the product such as color, size and so on. It used to make client able to choose desired product.
case class Parameter (
  name: String,
  paramGroup: String,
  description: Option[String] = None,
  priceOffset: Double
)

// Products https://my.ecwid.com/store/11719750#product:mode=new
case class Product (
  id: Option[Long] = None,
  name: String,
  vendorCode: Option[String] = None,
  weight: Double,
  deliveryType: Int,
  pictureUrl: String,
  price: Double,
  attributes: Map[String, Any] = Map.empty[String, Any],
  parameters: Seq[Parameter] = Seq.empty[Parameter],
  tags: Seq[String] = Seq.empty[String]
)

case class Category (
  id: Option[Long] = None,
  name: String,
  parentCategory: Option[Category] = None
)

case class Order (
  id: Option[Long] = None,
  orderNum: String,
  client: Client,
  deliveryAddress: Address,
  products: Seq[Product],
  orderSum: Double,
  paymentType: String,
  paymentStatus: String,
  orderStatus: String,
  discountCoupon: String,
  discountSum: Double,
  partnerId: String
)
