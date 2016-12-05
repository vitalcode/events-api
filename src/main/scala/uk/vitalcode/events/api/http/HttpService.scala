package uk.vitalcode.events.api.http

import akka.http.scaladsl.server.Directives._
import sangria.schema.Schema
import uk.vitalcode.events.api.http.routes.EventsServiceRoute
import uk.vitalcode.events.api.services.{AuthService, UsersService}
import uk.vitalcode.events.api.utils.CorsSupport

import scala.concurrent.ExecutionContext

class HttpService(usersService: UsersService,
                  authService: AuthService,
                  eventSchema: Schema[GraphqlContext, Unit])(implicit executionContext: ExecutionContext) extends CorsSupport {

  val graphQLRoute = new EventsServiceRoute(eventSchema)
  val routes = pathPrefix("v1") {
    corsHandler {
      graphQLRoute.route
    }
  }
}
