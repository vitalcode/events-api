package uk.vitalcode.events.api.http

import uk.vitalcode.events.api.models.TokenEntity
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GraphqlContext(val authService: AuthService, val usersService: UsersService, val eventService: EventService) {

  var token: Option[TokenEntity] = None

  def setToken(t: Option[TokenEntity]) = this.token = t

  def getAndSetToken(t: Option[String]) = setToken(t.flatMap(r => Await.result(authService.getToken(r), Duration.Inf)))
}

// TODO Duration fix