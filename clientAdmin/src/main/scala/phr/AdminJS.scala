package phr

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

@JSExportTopLevel(name = "rphAdminApp")
object AdminJS {
  @JSExport
  def main: Unit = {
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
