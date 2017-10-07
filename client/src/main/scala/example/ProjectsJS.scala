package example

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{Element, XMLHttpRequest}
import org.scalajs.jquery.{JQueryAjaxSettings, _}

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scalatags.Text.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.Try

@JSExportTopLevel(name = "rphApp")
object ProjectJS {
  @JSExport
  def main: Unit = {
    dom.window.addEventListener("hashchange", { (event: dom.HashChangeEvent) =>
      callLoad(event.newURL)
      event.preventDefault()
    }, false)

    // Connect to search field
    jQuery("#search-field").keyup((event: dom.KeyboardEvent) => onSearch(event))
    jQuery("#search-button").click(() => search())

    // Calling first time function
    callLoad(dom.window.location.hash)
  }

  def dynGet[T] (dyn: js.Dynamic, name:String): Option[T] = {
    val res = dyn.selectDynamic(name)
    if (!js.isUndefined (res) && res != null) Some(res.asInstanceOf[T]) else None
  }

  @JSExport
  def findAll (pageSize: Int, offset: Int, search: String, csrfHeader: String, csrfValue: String, linkText: String) = {
    var url = if (search != null && search != "") s"/drugs/fuzzySearch?searchText=$search&offset=$offset&pageSize=$pageSize"
    else s"/drugs/prod?offset=$offset&pageSize=$pageSize"

    val xhr = new XMLHttpRequest()
    xhr.open ("POST", url)

    xhr.onload = { (e: Event) =>
      if (xhr.status == 200) {
        val page = JSON.parse(xhr.responseText)
        val offsetJS:Int = dynGet[Int] (page, "offset").get
        val pageSizeJS = dynGet[Int] (page, "pageSize").get
        val hasMore = dynGet[Boolean] (page, "hasMore").get
        val rows = dynGet[js.Array[js.Dynamic]] (page, "rows").get
        val realSearch = search
        val seoDescription:StringBuffer = new StringBuffer("")

        val htmlRow = for (prj <- rows) yield {
          val fullName:String = dynGet[String] (prj, "drugsFullName").getOrElse("")
          val price:Double = dynGet[Double] (prj, "retailPrice").getOrElse(0)

          dynGet[String] (prj, "drugsShortName") match {
            case Some(name) => seoDescription.append(name.split("[ ,.]+")(0)).append(" цена: ").append(price).append(".00р, ")
            case _ =>
          }

          val mnn:String = dynGet[String] (prj, "MNN").getOrElse("")

          div (cls:="col-lg-3 col-sm-2 item")(
            div (cls:="panel panel-primary")(
              //div (cls:="panel-heading")(),
              div (cls:="panel-body")(
                img (cls:="img-responsive", src:=s"/assets/images/${dynGet[String] (prj, "drugImage").getOrElse("/nophoto.jpg")}"),
                //p (`class`:="memberNameLabel")(mnn),
                p (`class`:="description")(fullName)
                //a (`class`:="memberNameLink", href:=s"/project/${prj}")(linkText)
              ),
              div (cls:="panel-footer")(
                s"Цена: ${price}.00 р",
                button (
                  cls:="btn",
                  style:="float: right; margin: 0",
                  role:="button"
                )("В корзину")
              )
            )
          )
        }

        dom.document.querySelector(".row.drugs").innerHTML = htmlRow.map (_.render).mkString("")
        jQuery("meta[name=description]").attr("content", seoDescription.toString)

        // Set next button
        if (hasMore) {
          val nodeList = dom.document.querySelectorAll ("li.next")
          for (i <- 0 until nodeList.length) {
            val elem = nodeList.item(i)
            elem.asInstanceOf[Element].classList.remove("disabled")
            elem.firstChild.asInstanceOf[Element].setAttribute("href", s"#search=$realSearch,pageSize=$pageSizeJS,offset=${offsetJS + pageSizeJS}")
          }
        } else {
          val nodeList = dom.document.querySelectorAll ("li.next")
          for (i <- 0 until nodeList.length) {
            val elem = nodeList.item(i)
            elem.asInstanceOf[Element].classList.add("disabled")
            elem.firstChild.asInstanceOf[Element].setAttribute("href", s"javascript:void(0);")
          }
        }

        // Set prev button
        if (offsetJS > 0) {
          val realOffset = if (offsetJS - pageSizeJS < 0) 0 else offsetJS - pageSizeJS
          val nodeList = dom.document.querySelectorAll ("li.previous")
          for (i <- 0 until nodeList.length) {
            val elem = nodeList.item(i)
            elem.asInstanceOf[Element].classList.remove("disabled")
            elem.firstChild.asInstanceOf[Element].setAttribute("href", s"#search=$realSearch,pageSize=$pageSizeJS,offset=$realOffset")
          }
        } else {
          val nodeList = dom.document.querySelectorAll ("li.previous")
          for (i <- 0 until nodeList.length) {
            val elem = nodeList.item(i)
            elem.asInstanceOf[Element].classList.add("disabled")
            elem.firstChild.asInstanceOf[Element].setAttribute("href", s"javascript:void(0);")
          }
        }
      } else  {
        println(s"error: ${xhr.status}, ${xhr.statusText}")
      }
    }

    xhr.setRequestHeader(csrfHeader, csrfValue)
    xhr.send()
  }

  val pattern = "[#|,]([\\w|=]+=[^,]*)+".r
  def parseHash (url: String): Map[String, String] = {
    Try (
      if (!url.isEmpty) (pattern findAllMatchIn url).map( m => {
        val splits = m.group(1).split("=")
        if (splits.length == 2) Map[String, String](splits(0) -> splits(1)) else Map.empty[String, String]
      }).reduce ((a, b) => {
        a ++ b
      })
      else Map.empty[String, String]
    ).getOrElse(Map.empty[String, String])
  }

  private val DEFAULT_PAGE_SIZE = "8"

  def callLoad(url: String): Unit = {
    val load = js.Dynamic.global.selectDynamic("load")
    if (!js.isUndefined(load)) {
      val params = parseHash(url)
      val offset: Int = params.getOrElse("offset", "0").toInt
      val pageSize: Int = params.getOrElse("pageSize", DEFAULT_PAGE_SIZE).toInt
      val search: String = params.getOrElse("search", "")
      js.Dynamic.global.applyDynamic("load")(pageSize, offset, search)
    }
  }

  var timer:SetTimeoutHandle = null
  def onSearch (event: dom.KeyboardEvent) = {
    import scala.scalajs.js.timers._
    import scala.concurrent.duration._

    clearTimeout(timer);
    timer = setTimeout(500 millisecond) (search)
  }

  /**
    * Function seaches for drugs. Just changes current window hash
    */
  def search () = {
    val stringToSearch = jQuery("#search-field").value.toString.trim
    val urlEncodedString = js.Dynamic.global.applyDynamic("encodeURIComponent")(stringToSearch)
    val params = parseHash(dom.window.location.hash)
    val pageSize: Int = params.getOrElse("pageSize", DEFAULT_PAGE_SIZE).toInt
    dom.window.location.hash = s"search=$urlEncodedString,offset=0,pageSize=$pageSize"
  }

  @JSExportTopLevel(name = "pwdStrong")
  def pwdStrong () = {
    import org.scalajs.jquery.jQuery

    val password = jQuery("""[data-pwd="true"]""")
    if (password != null) {
      val meter = jQuery("#password-strength-meter")
      val msg = jQuery("#password-strength-text")

      password.change(e => showFeedback(e));
      password.keyup(e => showFeedback(e));
    }
  }

  def showFeedback(event:JQueryEventObject) = {
    val pwd = event.target.asInstanceOf[Input].value
    val result = js.Dynamic.global.applyDynamic("zxcvbn")(pwd)
    jQuery("#password-strength-meter").value(result.selectDynamic("score").toString);
  }
}
