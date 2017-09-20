package modules

import com.google.inject.AbstractModule
import models.daos._
import net.codingwell.scalaguice.ScalaModule

/**
  * Shop DAO classes configuration
  */
class ShopModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  def configure(): Unit = {
    bind[ProductDAO].to[ProductDAOImpl]
    bind[DrugsGroupDAO].to[DrugsGroupDAOImpl]
  }
}
