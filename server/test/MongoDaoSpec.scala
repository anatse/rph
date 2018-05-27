import java.util.UUID
import java.util.concurrent.Executors

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import de.flapdoodle.embed.mongo.{Command, MongodStarter}
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net, RuntimeConfigBuilder}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.IRuntimeConfig
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.runtime.Network
import models.User
import models.daos.UserDAOImpl
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.TestData
import org.slf4j.LoggerFactory
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.{DefaultReactiveMongoApi, ReactiveMongoApi}
import reactivemongo.api.gridfs.GridFS
import reactivemongo.play.json.JSONSerializationPack
import utils.auth.DefaultEnv

import concurrent.ExecutionContext.Implicits.global

class MongoDaoSpec extends PlaySpec with GuiceOneAppPerTest {

  class EmbeddedMongo(port: Int = 9999) {
    lazy val logger = LoggerFactory.getLogger(classOf[EmbeddedMongo])
    val nodes = List(s"localhost:$port")

    lazy val driver = new MongoDriver()
    private lazy val connection = driver.connection(nodes)
    private lazy val db: Future[DefaultDB] = connection.database("shopdb")

    lazy val mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(port, Network.localhostIsIPv6()))
      .build

    lazy val runtimeConfig: IRuntimeConfig = new RuntimeConfigBuilder()
      .defaultsWithLogger(Command.MongoD, logger)
      .processOutput(ProcessOutput.getDefaultInstanceSilent)
      .build;

    lazy val runtime = MongodStarter.getInstance(runtimeConfig)
    lazy val mongodExecutable = runtime.prepare(mongodConfig)

    def start = mongodExecutable.start
    def stop = mongodExecutable.stop
  }

  val identity = User(
      userID = UUID.randomUUID(),
      providerID = Some("facebook"),
      providerKey = Some("user@facebook.com"),
      firstName = None,
      lastName = None,
      fullName = None,
      email = None,
      avatarURL = None,
      activated = true,
      roles = None)

  val li = LoginInfo (identity.providerID.get, identity.providerKey.get)
  implicit val env = new FakeEnvironment[DefaultEnv](Seq(li -> identity))

  class FakeModule extends AbstractModule with ScalaModule {
    override def configure() = {
      bind[Environment[DefaultEnv]].toInstance(env)
    }
  }


  // Create application object with test config
  implicit override def newAppForTest(testData: TestData): Application = new GuiceApplicationBuilder().
      overrides(new FakeModule).
      loadConfig(conf = {
        val testConfig = ConfigFactory.load("application.test.conf")
        Configuration(testConfig)
      }).
      build()

  // Start embedded mongo
  System.out.println ("mongo starting...")
  val mongoServer = new EmbeddedMongo()
  mongoServer.start

  System.out.println ("mongo started")

  "MongoDao service" should {

    "Connect to users storage" in {
      // app.injector.instanceOf[ReactiveMongoApi]
      val userDao = new UserDAOImpl(app.injector.instanceOf[ReactiveMongoApi])
      val savedUserFuture = userDao.save(identity)
      val savedUser =  Await.result(savedUserFuture, 10 second)
      savedUser.providerID mustBe identity.providerID
      savedUser.providerKey mustBe identity.providerKey
      savedUser.userID mustBe identity.userID
    }
  }

  // Stop embedded mongo
//  mongoServer.stop
}