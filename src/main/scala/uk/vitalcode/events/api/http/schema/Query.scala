package uk.vitalcode.events.api.http.schema

import sangria.schema.{Field, ListType, ObjectType, OptionType, _}
import uk.vitalcode.events.api.http.{Authorised, AuthContext}
import uk.vitalcode.events.api.models.{Event, Page, UserEntity}
import uk.vitalcode.events.api.services.{EventService, UsersService}

trait Query extends DateType with EventCategoryType {

  val usersService: UsersService
  val eventService: EventService

  private val ID = Argument("id", StringType, description = "id of the character")
  private val Date = Argument("date", OptionInputType(DateType), description = "event search date")
  private val Clue = Argument("clue", OptionInputType(StringType), description = "event search clue")
  private val CategoryArg = Argument("category", OptionInputType(EventCategoryType), description = "event search category")
  private val Start = Argument("start", IntType, description = "event list start")
  private val Limit = Argument("limit", IntType, description = "event list limit")

  private val User = ObjectType("User", "Just User",
    fields = fields[Unit, UserEntity](
      Field("id", OptionType(LongType),
        Some("The id of the user"),
        resolve = _.value.id
      ),
      Field("username", StringType,
        Some("The name of the user"),
        resolve = _.value.username
      )
    )
  )

  private val Event = ObjectType(
    "Event",
    "Just Event",
    fields = fields[Unit, Event](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id),
      Field("category", OptionType(ListType(EventCategoryType)),
        Some("event category"),
        resolve = _.value.category),
      Field("description", OptionType(ListType(StringType)),
        Some("event description"),
        resolve = _.value.description),
      Field("from", OptionType(ListType(DateType)),
        Some("event from"),
        resolve = _.value.from
      )
    )
  )

  private val Page = ObjectType(
    "Page",
    "List page",
    fields = fields[Unit, Page[Event]](
      Field("total", IntType,
        Some("page items total count"),
        resolve = _.value.total),
      Field("items", OptionType(ListType(Event)),
        Some("page items"),
        resolve = _.value.items)
    )
  )

  val query = ObjectType("Query", fields[AuthContext, Unit](
    Field("me", OptionType(User),
      tags = Authorised :: Nil,
      resolve = ctx => ctx.ctx.subject
    ),
    Field("users", ListType(User),
      tags = Authorised :: Nil,
      resolve = ctx => usersService.getUsers
    ),
    Field("event", Event,
      arguments = ID :: Nil,
      tags = Authorised :: Nil,
      resolve = ctx => eventService.getEvent(ctx arg ID).get),
    Field("events", Page,
      arguments = Date :: Clue :: CategoryArg :: Start :: Limit :: Nil,
      tags = Authorised :: Nil,
      resolve = ctx => eventService.getEvents(
        date = ctx.arg(Date),
        clue = ctx.arg(Clue),
        category = ctx.arg(CategoryArg),
        start = ctx.arg(Start),
        limit = ctx.arg(Limit)).get)
  ))
}
