package uk.vitalcode.events.api.http

import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}
import uk.vitalcode.events.api.utils.JwtUtils


class GraphqlContext(val authService: AuthService, val usersService: UsersService, val eventService: EventService) {

  var subject: Option[UserEntity] = None

  def setSubject(t: Option[UserEntity]) = this.subject = t

  def getAndSetSubject(t: Option[String]) = setSubject(t.flatMap(JwtUtils.decode))
}
