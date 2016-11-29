package uk.vitalcode.events.api.http

import uk.vitalcode.events.api.models.{AuthorisationException, TokenEntity, UserEntity}
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class EventContext(authService: AuthService,
                        usersService: UsersService,
                        eventService: EventService) {

  var token: Option[TokenEntity] = None

  def setToken(t: Option[TokenEntity]) = this.token

  def getAndSetToken(t: Option[String]) = this.token = t.flatMap(r => Await.result(authService.getToken(r), Duration.Inf))

  //  def login2(userName: String, password: String) = Await.result(authService.login(userName, password), Duration.Inf) getOrElse (
  //    throw new AuthenticationException("UserName or password is incorrect"))


  def authorised[T](permissions: String*)(fn: UserEntity ⇒ T) =
    token.flatMap(t => Await.result(authService.authorise(t.token), Duration.Inf)).fold(throw AuthorisationException("Invalid token (authorised)")) {
      user ⇒
        if (permissions.forall(user.permissions.contains)) fn(user)
        else throw AuthorisationException("You do not have permission to do this operation")
    }
}

// TODO Duration fix