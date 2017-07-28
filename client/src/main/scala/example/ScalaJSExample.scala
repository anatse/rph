package example

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.DragEvent
import shared.SharedMessages

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport(name = "rphApp")
object ScalaJSExample extends js.JSApp {
  def main(): Unit = {
    //dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
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
