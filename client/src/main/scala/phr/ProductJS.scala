package phr

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.{Element, XMLHttpRequest}
import org.scalajs.jquery.{JQueryAjaxSettings, _}
import shared.ShopCartItem

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import scalatags.Text.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.Try

@JSExportTopLevel(name = "rphApp")
object ProductJS {
  @JSExport
  def main: Unit = {
    dom.window.addEventListener("hashchange", { (event: dom.HashChangeEvent) =>
      callLoad(dom.window.location.hash)
      event.preventDefault()
    }, false)

    // Connect to search field
    jQuery("#search-field").keyup((event: dom.KeyboardEvent) => onSearch(event))
    jQuery("#search-button").click(() => search())

    // Calling first time function
    callLoad(dom.window.location.hash)

    modalSetLocation ("#myModal", "#backRedirect")
  }

  def dynGet[T] (dyn: js.Dynamic, name:String): Option[T] = {
    val res = dyn.selectDynamic(name)
    if (!js.isUndefined (res) && res != null) Some(res.asInstanceOf[T]) else None
  }

  private val incartClass = "incart"

  def addItem(csrfHeader: String, csrfValue: String, inp:JQuery, shopCartItem: ShopCartItem, reload: Boolean = false) = {
    var url = "/drugs/cart/item"
    val xhr = new XMLHttpRequest()
    xhr.open ("POST", url)
    xhr.setRequestHeader(csrfHeader, csrfValue)
    xhr.onload = {(e:Event) => {
      if (xhr.status == 200) {
        val resp = JSON.parse(xhr.responseText)
        val cart = dynGet[js.Dynamic](resp, "cart").get
        val rows = dynGet[js.Array[js.Dynamic]](cart, "items").get

        val badge = jQuery("#cart-badge")
        if (badge.length == 0) {
          dom.window.location.reload(true)
        }
        else {
          jQuery("#cart-badge").text(s"${rows.length}")

          if (reload) {
            dom.window.location.reload(true)
          }
          else {
            val foundDrugs = for (row <- rows) yield {
              val drugId = dynGet[String](row, "drugId").get
              val num = dynGet[Int](row, "num").get
              val foundBtn = jQuery(s"#$drugId")

              if (foundBtn.length == 1) {
                val num = dynGet[Int](row, "num").get

                if (num > 0)
                  addClass(drugId)

                jQuery(s"#$drugId input").value(s"$num")
                drugId
              } else null
            }.filter(id => id != null)

            val incarts = jQuery(s"div.$incartClass")
            for (index <- 0 to incarts.length) {
              val id = jQuery(incarts.get(index)).attr("id")
              if (id != js.undefined && !foundDrugs.contains(id.asInstanceOf[String])) {
                val finp = jQuery(incarts.get(index))
                removeClass(id.asInstanceOf[String])
                jQuery(s"#$id input").value("0")
              }
            }
          }
        }
      }
    }}

    val num = shopCartItem.num + (if (inp != null) inp.value().asInstanceOf[String].toInt else 0)
    xhr.send(JSON.stringify(js.Dynamic.literal(
      drugId = shopCartItem.drugId,
      drugName = shopCartItem.drugName,
      num = (if (num < 0) 0 else num),
      price = shopCartItem.price
    )))
  }

  def addClass (drugId: String) = {
    jQuery(s"#$drugId").addClass(incartClass)
    jQuery(s"#$drugId span:contains('+')").addClass(incartClass)
    jQuery(s"#$drugId span:contains('-')").addClass(incartClass)
    jQuery(s"#$drugId span:contains('-')").removeClass("hide")
  }

  def removeClass (drugId: String) = {
    jQuery(s"#$drugId").removeClass(incartClass)
    jQuery(s"#$drugId span:contains('+')").removeClass(incartClass)
    jQuery(s"#$drugId span:contains('-')").addClass("hide")
  }

  def cartButon(drugId: String, num: Int) = {
    div(cls:="col-lg-1 input-group input-group-sm", style:="float:right", id:=drugId)(
      span(cls:=s"input-group-addon btn ${if (num > 0) incartClass else "hide"}")("-"),
      input(`type`:="text", readonly:=true, cls:=s"form-control cart-btn ${if (num > 0) incartClass else ""}", value:=s"$num"),
      span(cls:=s"input-group-addon btn ${if (num > 0) incartClass else ""}")("+")
    )
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

        val msg = jQuery("#messages")
        val priceMsg = msg.attr("price-msg")
        val putInCart = msg.attr("put-in-cart")
        val inCart = msg.attr("in-cart")

        val htmlRow = for (drugRs <- rows) yield {
          val countInCart = dynGet[Int] (drugRs, "countInCart").getOrElse(0)
          val drug = drugRs.selectDynamic("dp")

          val fullName:String = dynGet[String] (drug, "drugsFullName").getOrElse("")
          val price:Double = dynGet[Double] (drug, "retailPrice").getOrElse(0)
          val producerShortName:String = dynGet[String] (drug, "producerShortName").getOrElse("")
          val drugId:String = dynGet[String] (drug, "id").getOrElse("")

          dynGet[String] (drug, "drugsShortName") match {
            case Some(name) => seoDescription.append(name.split("[ ,.]+")(0)).append(s" $priceMsg: ").append(price).append(".00р, ")
            case _ =>
          }

          val mnn:String = dynGet[String] (drug, "MNN").getOrElse("")

          div (cls:="col-lg-3 col-sm-2 item")(
            div (cls:="panel panel-primary")(
              div (cls:="panel-body")(
                img (cls:="img-responsive", src:=s"/assets/images/${dynGet[String] (drug, "drugImage").getOrElse("nophoto.png")}"),
                p (`class`:="description")(fullName),
                p (`class`:="producer")(producerShortName)
              ),
              div (cls:="panel-footer")(
                s"$priceMsg: ${price}.00 р",
                cartButon(drugId, countInCart)
              )
            )
          )
        }

        dom.document.querySelector(".row.drugs").innerHTML = htmlRow.map (_.render).mkString("")
        jQuery("meta[name=description]").attr("content", seoDescription.toString)

        rows.toList.foreach(drugRs => {
          val drug = drugRs.selectDynamic("dp")
          val drugId:String = dynGet[String] (drug, "id").getOrElse("")
          val find = s"#${drugId}"
          val fullName:String = dynGet[String] (drug, "drugsFullName").getOrElse("")
          val price:Double = dynGet[Double] (drug, "retailPrice").getOrElse(0)

          val inp = jQuery(s"#$drugId input")
          jQuery(s"#$drugId span:contains('-')").click((event: Event) => addItem(csrfHeader, csrfValue, inp, ShopCartItem(drugId, fullName, -1, price)))
          jQuery(s"#$drugId span:contains('+')").click((event: Event) => addItem(csrfHeader, csrfValue, inp, ShopCartItem(drugId, fullName, 1, price)))
        })

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

  @JSExport
  def modalSetLocation (modal: String, field: String) = jQuery(modal).bind("show.bs.modal", (event: Event) => {
    val url = s"${dom.window.location.pathname}${dom.window.location.hash}"
    jQuery(s"$modal $field").value(s"$url")
  })

  @JSExport
  def updateCartItem (csrfHeader: String, csrfValue: String, event: Event, drugId: String): Unit = {
    val num = jQuery(s"#num_$drugId").value.asInstanceOf[String]
    addItem(csrfHeader, csrfValue, null, ShopCartItem(drugId, "", num.toInt, 0), true)
  }
}
