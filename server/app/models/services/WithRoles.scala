package models.services

import concurrent.Future
import com.mohiva.play.silhouette.api.{Authenticator, Authorization}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.mvc.Request

trait LogicalConj {}

case class WithRoles(roles: String*) extends Authorization[User, CookieAuthenticator] {
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(
    implicit request: Request[B]) = {
      Future.successful(user.roles match {
        case Some(r) => !roles.intersect(r).isEmpty
        case _ => false
    })
  }
}
