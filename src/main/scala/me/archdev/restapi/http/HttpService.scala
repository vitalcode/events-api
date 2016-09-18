package me.archdev.restapi.http

import akka.http.scaladsl.server.Directives._
import me.archdev.restapi.http.routes.{AuthServiceRoute, EventRepo, GrahpQL, UsersServiceRoute}
import me.archdev.restapi.services.{AuthService, UsersService}
import me.archdev.restapi.utils.CorsSupport

import scala.concurrent.ExecutionContext

class HttpService(usersService: UsersService,
                  authService: AuthService,
                  eventRepo: EventRepo
                 )(implicit executionContext: ExecutionContext) extends CorsSupport {

  val usersRouter = new UsersServiceRoute(authService, usersService)
  val graphQLRoute = new GrahpQL(authService, eventRepo)
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
