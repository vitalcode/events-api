package me.archdev.restapi.http

import akka.http.scaladsl.server.Directives._
import me.archdev.restapi.http.routes._
import me.archdev.restapi.services.{AuthService, UsersService}
import me.archdev.restapi.utils.CorsSupport

import scala.concurrent.ExecutionContext

class HttpService(usersService: UsersService,
                  authService: AuthService,
                  eventContext: EventContext
                 )(implicit executionContext: ExecutionContext) extends CorsSupport {

  val usersRouter = new UsersServiceRoute(authService, usersService)
  val graphQLRoute = new EventsServiceRoute(eventContext)
  val authRouter = new AuthServiceRoute(authService)

  val routes =
    pathPrefix("v1") {
      corsHandler {
        usersRouter.route ~
          authRouter.route ~
          graphQLRoute.route
      }
    }
}
