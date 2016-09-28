package uk.vitalcode.events.api.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import uk.vitalcode.events.api.http.SecurityDirectives
import uk.vitalcode.events.api.models.UserEntityUpdate
import uk.vitalcode.events.api.services.{AuthService, UsersService}

import scala.concurrent.ExecutionContext

class UsersServiceRoute(val authService: AuthService,
                        usersService: UsersService
                       )(implicit executionContext: ExecutionContext) extends CirceSupport with SecurityDirectives {

  import StatusCodes._
  import usersService._

  val route = pathPrefix("users") {
    pathEndOrSingleSlash {
      get {
        complete(getUsers.map(_.asJson))
      }
    } ~
      pathPrefix("me") {
        pathEndOrSingleSlash {
          authenticate { loggedUser =>
            get {
              complete(loggedUser)
            } ~
              post {
                entity(as[UserEntityUpdate]) { userUpdate =>
                  complete(updateUser(loggedUser.id.get, userUpdate).map(_.asJson))
                }
              }
          }
        }
      } ~
      pathPrefix(IntNumber) { id =>
        pathEndOrSingleSlash {
          get {
            complete(getUserById(id).map(_.asJson))
          } ~
            post {
              entity(as[UserEntityUpdate]) { userUpdate =>
                complete(updateUser(id, userUpdate).map(_.asJson))
              }
            } ~
            delete {
              onSuccess(deleteUser(id)) { ignored =>
                complete(NoContent)
              }
            }
        }
      }
  }

}
