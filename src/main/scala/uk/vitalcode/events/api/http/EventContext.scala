package uk.vitalcode.events.api.http

import com.sksamuel.elastic4s.{ElasticClient, IndexType}
import uk.vitalcode.events.api.models.{TokenEntity, UserEntity}
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}
import uk.vitalcode.events.api.utils.DatabaseService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


case class AuthenticationException(message: String) extends Exception(message)

case class AuthorisationException(message: String) extends Exception(message)

case class EventContext(implicit val client: ElasticClient,
                        implicit val indexType: IndexType,
                        implicit val databaseService: DatabaseService,
                        implicit val executionContext: ExecutionContext) extends EventService with UsersService with AuthService {

  var token: Option[String] = None

  def setToken(t: Option[String]) = this.token = t

  def login(userName: String, password: String) = Await.result(signIn(userName, password), Duration.Inf) getOrElse (
    throw new AuthenticationException("UserName or password is incorrect"))

//  def signUp(login: String, pass: String): TokenEntity = {
//    val newUser = UserEntity(
//      username = login,
//      password = pass
//    )
//    Await.result(signup(newUser), Duration.Inf)
//  }

  def authorised[T](permissions: String*)(fn: UserEntity ⇒ T) =
    token.flatMap(t => Await.result(authorise(t), Duration.Inf)).fold(throw AuthorisationException("Invalid token (authorised)")) {
      user ⇒
        if (permissions.forall(user.permissions.contains)) fn(user)
        else throw AuthorisationException("You do not have permission to do this operation")
    }

  //  def ensurePermissions(permissions: List[String]): Unit =
  //    token.flatMap(t => Await.result(authorise(t), Duration.Inf)).fold(throw AuthorisationException("Invalid token (ensurePermissions)")) {
  //      user ⇒
  //        if (!permissions.forall(user.permissions.contains))
  //          throw AuthorisationException("You do not have permission to do this operation")
  //    }

  //  def user = {
  //    token.flatMap(t => Await.result(authorise(t), Duration.Inf)).fold(throw AuthorisationException("Invalid token (user)"))(identity)
  //  }
}

// TODO Duration fix