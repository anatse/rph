package controllers

import javax.inject._

import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient.{OrientDynaElementIterable, OrientGraph, OrientGraphFactory, OrientVertex}
import model.OrientVertexId
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.filters.csrf.CSRF.Token
import play.filters.csrf._

import scala.util.Try
import scala.collection.JavaConverters._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 * https://github.com/mpollmeier/gremlin-scala
 * https://github.com/coreyauger/reactive-gremlin
  *
  * insert into cluster:u_patient(name, email)  values ('ai-bolit', 'ai-bolit@rph.ru')
  *
  * WS calls https://www.playframework.com/documentation/2.5.x/ScalaWS
  * OpenID https://www.playframework.com/documentation/2.5.x/ScalaOpenID
 *
 */
@Singleton
class HomeController @Inject()(components: ControllerComponents, webJarAssets: WebJarAssets, addToken: CSRFAddToken, checkToken: CSRFCheck)
  extends AbstractController(components) with I18nSupport {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = addToken {
    Action { implicit request =>
      val Token(name, value) = CSRF.getToken.get
      val p = OrientVertexId ("10:8")
      println(p)
      test
      Ok(views.html.index(webJarAssets, Messages("ready.msg"), name, value))
    }
  }

  def test = {
    println ("start test procedure")
    val uri = "remote:localhost/test"
    val graphFactory = new OrientGraphFactory(uri, "root", "root")
    Try (graphFactory.getTx) map { graph =>
      val results: OrientDynaElementIterable = graph
        .command(new OCommandSQL(s"SELECT * FROM v"))
        .execute()

      println ("Query statement executed")
      results.asScala.foreach (v => {
        val person = v.asInstanceOf[OrientVertex]
        println(s"Name: ${person.getProperty("name")}, ${person.getProperty("type")}")
      })

      graph.shutdown()
    }

    //val simple = GremlinClient.buildRequest("g.V().has('email','donald@trumpdonald.org').has('is_douchebag','true').valueMap();")

  }
}
