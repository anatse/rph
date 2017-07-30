package example

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{DragEvent}
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel(name = "rphApp")
object ScalaJSExample extends js.JSApp {
  def main(): Unit = {
//    MyStyles.render[String]
//    val b =  div(cls := "mystyle",
//      //MyStyles.render, //[scalatags.JsDom.TypedTag[HTMLStyleElement]],
//      "I am a button!"
//    )
//
//    dom.document.body.appendChild(b.render)
  }

  @JSExport
  def allowDrop(event: Event): Unit = {
    event.preventDefault
  }

  @JSExport
  def drag (event: DragEvent): Unit = {
    val elem = event.target.asInstanceOf[dom.Element]
    event.dataTransfer.setData("text", elem.id)
  }

  @JSExport
  def drop (event: DragEvent): Unit = {
    event.preventDefault
    val data = event.dataTransfer.getData("text")
    val elem = event.target.asInstanceOf[dom.Element]
    elem.appendChild(dom.document.getElementById(data))
  }
}
