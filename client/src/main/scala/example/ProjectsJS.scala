package example

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{Element, XMLHttpRequest}
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scalatags.Text.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

@JSExportTopLevel(name = "rphApp")
object ProjectJS {
  @JSExport
  def main: Unit = {
    dom.window.addEventListener("hashchange", { (event: dom.HashChangeEvent) =>
      callLoad(event.newURL)
      event.preventDefault()
    }, false)

    // Connect to search field
    dom.document.getElementById("search-field").addEventListener("keyup", onSearch)

    // Calling first time function
    callLoad(dom.window.location.hash)
  }

  @JSExport
  def findAll (pageSize: Int, offset: Int, csrfHeader: String, csrfValue: String, linkText: String) = {
    val xhr = new XMLHttpRequest()

    xhr.open("POST", s"/projects/list?offset=$offset&pageSize=$pageSize")
    xhr.onload = { (e: Event) =>
      if (xhr.status == 200) {
        val page = JSON.parse(xhr.responseText)
        val offsetJS = page.selectDynamic("offset").asInstanceOf[Int]
        val pageSizeJS = page.selectDynamic("pageSize").asInstanceOf[Int]
        val hasMore = page.selectDynamic("hasMore").asInstanceOf[Boolean]
        val rows = page.selectDynamic("rows").asInstanceOf[js.Array[js.Dynamic]]

        val htmlRow = for (prj <- rows) yield {
          div (cls:="col-lg-3 col-sm-2 item")(
            div (cls:="panel panel-primary")(
              div (cls:="panel-heading")(prj.selectDynamic("name").asInstanceOf[String]),
              div (cls:="panel-body")(
                img (cls:="img-responsive", src:="/assets/img/desk.jpg"),
                h4 (`class`:="memberNameLabel")(prj.selectDynamic("number").asInstanceOf[String]),
                p (`class`:="description")(prj.selectDynamic("description").asInstanceOf[String]),
                a (`class`:="memberNameLink", href:=s"/project/${prj.number}")(linkText)
              ),
              div (cls:="panel-footer")("Buy 10$")
            )
          )
        }

        dom.document.querySelector(".row.projects").innerHTML = htmlRow.map (_.render).mkString("")

        // Set next button
        if (hasMore) {
          val nodeList = dom.document.querySelectorAll ("li.next")
          for (i <- 0 until nodeList.length) {
            val elem = nodeList.item(i)
            elem.asInstanceOf[Element].classList.remove("disabled")
            elem.firstChild.asInstanceOf[Element].setAttribute("href", s"#pageSize=$pageSizeJS,offset=${offsetJS + pageSizeJS}")
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
            elem.firstChild.asInstanceOf[Element].setAttribute("href", s"#pageSize=$pageSizeJS,offset=$realOffset")
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

  val pattern = "[#|,]([\\w|=]+=\\w*)+".r
  def parseHash (url: String): Map[String, String] = {
    if (!url.isEmpty) (pattern findAllMatchIn url).map( m => {
      val splits = m.group(1).split("=")
      Map[String, String](splits(0) -> splits(1))
    }).reduce ((a, b) => {
      a ++ b
    })
    else Map.empty
  }

  def callLoad (url: String): Unit = {
    val load = js.Dynamic.global.selectDynamic("load")
    if (!js.isUndefined(load)) {
      val params = parseHash(url)
      val offset: Int = params.getOrElse("offset", "0").toInt
      val pageSize: Int = params.getOrElse("pageSize", "4").toInt
      js.Dynamic.global.applyDynamic("load")(pageSize, offset)
    }
  }

  def onSearch (event: dom.KeyboardEvent) = {
    val stringToSearch = event.target.asInstanceOf[Input].value
    dom.console.log(stringToSearch)
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
