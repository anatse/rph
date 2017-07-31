import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{ Environment, LoginInfo }
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test._
import play.api.test.Helpers._
import utils.auth.DefaultEnv
import com.mohiva.play.silhouette.test._
import com.typesafe.config.ConfigFactory
import models.User
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.TestData
import play.api.{ Application, Configuration }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper._
import controllers.routes._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with GuiceOneAppPerTest {
  val identity = User(
    userID = UUID.randomUUID(),
    loginInfo = LoginInfo("facebook", "user@facebook.com"),
    firstName = None,
    lastName = None,
    fullName = None,
    email = None,
    avatarURL = None,
    activated = true)

  implicit val env: Environment[DefaultEnv] = new FakeEnvironment[DefaultEnv](Seq(identity.loginInfo -> identity))

  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
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

  "Application controller" should {
    "Redirect to login page if user is unauthorized " in {
      val Some(redirectResult) = route(app, FakeRequest(ApplicationController.index).withAuthenticator[DefaultEnv](LoginInfo("invalid", "invalid")))

      status(redirectResult) mustBe (SEE_OTHER)

      val redirectURL = redirectLocation(redirectResult).getOrElse("")

      redirectURL must startWith("/sign")
      val Some(unauthorizedResult) = route(app, addCSRFToken(FakeRequest(GET, redirectURL)))

      status(unauthorizedResult) mustBe (OK)
      contentType(unauthorizedResult) mustBe (Some("text/html"))
      contentAsString(unauthorizedResult) must include("- Sign In")
    }

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

    "return 200 if user is authorized" in {
      val Some(result) = route(app, addCSRFToken(FakeRequest(ApplicationController.index).withAuthenticator[DefaultEnv](identity.loginInfo)))
      status(result) mustBe (OK)
    }
  }

  "Project controller" should {
//    "drop all tables" in {
//      var Some(result) = route (app, addCSRFToken(FakeRequest(ProjectController.dropAll).withAuthenticator[DefaultEnv](identity.loginInfo)))
//      status(result) mustBe (OK)
//
//      contentType(result) mustBe (Some("text/plain"))
//      contentAsString(result) must include("dropped")
//    }

    "create projects table" in {
      var Some(result) = route (app, addCSRFToken(FakeRequest(ProjectController.create).withAuthenticator[DefaultEnv](identity.loginInfo)))
      status(result) mustBe (OK)

      contentType(result) mustBe (Some("text/plain"))
      contentAsString(result) must include("created")
    }

    "create throws an exception when projects table already created" in {
      val caught = intercept[org.h2.jdbc.JdbcSQLException] {
        var Some(result) =route(app, addCSRFToken(FakeRequest(ProjectController.create).withAuthenticator[DefaultEnv](identity.loginInfo)))
        status(result) must not be(OK)
      }

      caught.getMessage must include("Table \"projects\" already exists")
    }

    "add new project" in {
      var Some(result) = route (app, addCSRFToken(FakeRequest(ProjectController.index).withAuthenticator[DefaultEnv](identity.loginInfo)))
      status(result) mustBe (OK)

      contentType(result) mustBe (Some("text/plain"))
      contentAsString(result) must include("inserted")
    }

    "list all projects" in {
      var Some(result) = route (app, addCSRFToken(FakeRequest(ProjectController.findAll).withAuthenticator[DefaultEnv](identity.loginInfo)))
      status(result) mustBe (OK)

      contentType(result) mustBe (Some("text/html"))
      contentAsString(result) must include("Projects list")
    }
  }
}
