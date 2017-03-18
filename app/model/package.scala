import akka.shapeless.HList

import scala.annotation.meta.getter
import scala.util.matching.Regex

/**
  * Created by asementsov on 17.03.17.
  */
package object model {

  /**
    * Class represents orient vertex id object
    * @param id identifier of vertex
    * @param cluster cluster identifier
    */
  case class OrientVertexId (id:String, cluster:String) {
    override def toString: String = id + ":" + cluster
  }

  object OrientVertexId {
    val pattern = "([0-9]+):([0-9]+)".r
    def apply (v:String) = {
      val pattern(id, cluster) = v
      new OrientVertexId(id, cluster)
    }
  }

  sealed trait BaseModelObject {
    val id: OrientVertexId
    val props:List[Property]
  }

  class ProdMetaData(dbname: String, required: Boolean, filterBy:Boolean) extends scala.annotation.Annotation

  /**
    * Class represents additional properties
    * @param name name of property
    * @param value value of property
    */
  case class Property (name:String, value:Any)

  case class prod (map:Map[String, AnyVal])

  /**
    * Class represents product, i.e. any type of subjects, main vertex in orientdb database
    * @param id identifier of product (id of vertex)
    * @param name name of product
    * @param description decription of product
    * @param props additional properties
    */
  case class Product (id:OrientVertexId,   @ProdMetaData ("name", true, false) name:String, description:String, props:List[Property]) extends BaseModelObject

  import scala.reflect.runtime.universe._

  val a = typeOf[Product].typeSymbol.annotations.head

  println (a)


  /**
    * Class represents role, e.g. merchant, client, vip_client, etc.
    * @param id identifier
    * @param name name of the role
    * @param props additional properties
    */
  case class Role(id:OrientVertexId, name:String, props:List[Property]) extends BaseModelObject

  /**
    * Class for user object
    * @param id identifier
    * @param name name of the user
    * @param email email
    * @param roles roles fo current user
    * @param props additional properties
    */
  case class User(id:OrientVertexId, name:String, email:String, roles:List[Role], props:List[Property]) extends BaseModelObject


}
