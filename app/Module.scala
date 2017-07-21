import com.google.inject.AbstractModule
import java.time.Clock

import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.{FingerprintGenerator, IDGenerator, PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import services.{ApplicationTimer, AtomicCounter, Counter}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])


    // Configure Silhoulette
//    bind[IdentityService[User]].to[UserService]
//    bind[UserDao].to[MongoUserDao]
//    bind[UserTokenDao].to[MongoUserTokenDao]
//    bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordInfoDao]
//    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDao]
    bind(classOf[IDGenerator]).toInstance(new SecureRandomIDGenerator())
    bind(classOf[PasswordHasher]).toInstance(new BCryptPasswordHasher)
    bind(classOf[FingerprintGenerator]).toInstance(new DefaultFingerprintGenerator(false))
    bind(classOf[EventBus]).toInstance(EventBus())
  }

}
