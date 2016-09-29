package uk.vitalcode.events.api.http

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.{BasicDirectives, FutureDirectives, HeaderDirectives, RouteDirectives}
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.services.AuthService

trait SecurityDirectives {

  import BasicDirectives._
  import FutureDirectives._
  import HeaderDirectives._
  import RouteDirectives._

  def authenticate: Directive1[UserEntity] = {
    headerValueByName("Token").flatMap { token =>
      onSuccess(authService.authorise(token)).flatMap {
        case Some(user) => provide(user)
        case None => reject
      }
    }
  }

  protected val authService: AuthService

}
