package example

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{DragEvent, Element, XMLHttpRequest}
import org.scalajs.jquery._

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel, JSGlobal, JSImport}
import shared._

import scalatags.Text.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

@js.native
@JSGlobal
object jquery extends JQueryStatic

@JSExportTopLevel(name = "rphApp")
object ScalaJSExample extends js.JSApp {

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
          div (cls:="col-lg-4 col-sm-6 item")(
            img (cls:="img-responsive", src:="/assets/img/desk.jpg"),
            h3 (`class`:="name")(prj.selectDynamic("name").asInstanceOf[String]),
            h4 (`class`:="memberNameLabel")(prj.selectDynamic("number").asInstanceOf[String]),
            p (`class`:="description")(prj.selectDynamic("description").asInstanceOf[String]),
            a (`class`:="memberNameLink", href:=s"/project/${prj.number}")(linkText)
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
    (pattern findAllMatchIn url).map( m => {
      val splits = m.group(1).split("=")
      Map[String, String](splits(0) -> splits(1))
    }).reduce ((a, b) => {
      a ++ b
    })
  }

  def callLoad (url: String): Unit = {
    val params = parseHash(url)
    val offset:Int = params.getOrElse("offset", "0").toInt
    val pageSize:Int = params.getOrElse("pageSize", "3").toInt
    js.Dynamic.global.applyDynamic("load")(pageSize, offset)
  }

  override def main(): Unit = {
    dom.window.addEventListener("hashchange", { (event: dom.HashChangeEvent) =>
      callLoad(event.newURL)
      event.preventDefault()
    }, false)

    // Calling first time function
    callLoad(dom.window.location.hash)
  }
}
