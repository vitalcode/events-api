package uk.vitalcode.events.api.http

import akka.http.scaladsl.server.Directives._
import uk.vitalcode.events.api.http.routes.EventsServiceRoute
import uk.vitalcode.events.api.http.schema.EventServiceSchema
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}
import uk.vitalcode.events.api.utils.CorsSupport

import scala.concurrent.ExecutionContext

class HttpService(usersService: UsersService,
                  authService: AuthService,
                  eventService: EventService)(implicit executionContext: ExecutionContext) extends CorsSupport {

  private val eventSchema = new EventServiceSchema(usersService, authService, eventService).EventSchema
  val graphQLRoute = new EventsServiceRoute(eventSchema)

  val routes = pathPrefix("v1") {
    corsHandler {
      graphQLRoute.route
    }
  }
}
