package uk.vitalcode.events.api.http.schema

import sangria.schema._
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}


class EventServiceSchema(val usersService: UsersService,
                         val authService: AuthService,
                         val eventService: EventService) extends Mutation with Query {

  val EventSchema = Schema(query, Some(MutationType))
}
