package utils

import models.{ShopCart, ShopCartItem, User}
import play.api.mvc.Session

object CartUtil {
  /**
    * Function generate session UUID if it does not exists or return ones
    * @param session session
    * @return sesion uuid
    */
  def sessionId (session: Session):String = {
    session.get("uuid").getOrElse {
      java.util.UUID.randomUUID.toString
    }
  }

  /**
    * Function prepare cart from request
    * @return
    */
  def getCart (sid: String, ident:Option[User]): ShopCart = {
    ident match {
      case Some(user) => ShopCart(Some(user.userID.toString), sid, Array[ShopCartItem]())
      case None => ShopCart(None, sid, Array[ShopCartItem]())
    }
  }
}
