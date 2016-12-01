package uk.vitalcode.events.api.http

import akka.http.scaladsl.server.Directives._
import uk.vitalcode.events.api.http.routes.{AuthServiceRoute, EventsServiceRoute, UsersServiceRoute}
import uk.vitalcode.events.api.services.{AuthService, UsersService}
import uk.vitalcode.events.api.utils.CorsSupport

import scala.concurrent.ExecutionContext

class HttpService(usersService: UsersService,
                  authService: AuthService,
                  eventContext: GraphqlContext
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
