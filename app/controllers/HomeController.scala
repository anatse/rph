package controllers

import javax.inject._

import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient.{OrientDynaElementIterable, OrientGraph, OrientGraphFactory, OrientVertex}
import io.surfkit.gremlin.GremlinClient
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.filters.csrf.CSRF.Token
import play.filters.csrf._

import scala.util.Try
import scala.collection.JavaConversions._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 * https://github.com/mpollmeier/gremlin-scala
 * https://github.com/coreyauger/reactive-gremlin
 *
 */
@Singleton
class HomeController @Inject()(val messagesApi: MessagesApi, addToken: CSRFAddToken, checkToken: CSRFCheck) extends Controller with I18nSupport {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = addToken {
    Action { implicit request =>
      val Token(name, value) = CSRF.getToken.get
      test
      Ok(views.html.index(Messages("ready.msg"), name, value))
    }
  }

  def test = {
    val uri = "remote:localhost/GratefulDeadConcerts"
    val graphFactory = new OrientGraphFactory(uri, "root", "123456")
    Try (graphFactory.getTx) map { graph =>
      val results: OrientDynaElementIterable = graph
        .command(new OCommandSQL(s"SELECT * FROM v"))
        .execute()

      results.foreach (v => {
        val person = v.asInstanceOf[OrientVertex]
        println(s"Name: ${person.getProperty("name")}, ${person.getProperty("type")}")
      })

      graph.shutdown()
    }

    val simple = GremlinClient.buildRequest("g.V().has('email','donald@trumpdonald.org').has('is_douchebag','true').valueMap();")

  }
}
