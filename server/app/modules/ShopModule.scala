package modules

import com.cloudinary.Cloudinary
import com.google.inject.{AbstractModule, Provides}
import models.daos._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration

/**
  * Shop DAO classes configuration
  */
class ShopModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  override def configure(): Unit = {
    bind[ProductDAO].to[ProductDAOImpl]
    bind[DrugsGroupDAO].to[DrugsGroupDAOImpl]
    bind[CartDAO].to[CartDAOImpl]
  }

  @Provides
  def provideCloudinary(configuration: Configuration):Cloudinary = {
    new Cloudinary (configuration.underlying.getString("cloudinary.url"))
  }
}
