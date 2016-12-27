package uk.vitalcode.events.api.http.schema

import sangria.schema.{Field, ListType, ObjectType, OptionType, _}
import uk.vitalcode.events.api.http.{AuthContext, Authorised}
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
    "Event", "Just Event",
    fields = fields[Unit, Event](
      Field("id", StringType,
        Some("event id"),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id
      ),
      Field("url", OptionType(ListType(StringType)),
        Some("event url"),
        resolve = _.value.url
      ),
      Field("title", OptionType(ListType(StringType)),
        Some("event title"),
        resolve = _.value.title
      ),
      Field("from", OptionType(ListType(DateType)),
        Some("when event starts"),
        resolve = _.value.from
      ),
      Field("to", OptionType(ListType(DateType)),
        Some("when event finishes"),
        resolve = _.value.to
      ),
      Field("category", OptionType(ListType(EventCategoryType)),
        Some("event category"),
        resolve = _.value.category
      ),
      Field("description", OptionType(ListType(StringType)),
        Some("event description"),
        resolve = _.value.description
      ),
      Field("image", OptionType(ListType(StringType)),
        Some("event image url"),
        resolve = _.value.image
      ),
      Field("cost", OptionType(ListType(StringType)),
        Some("event cost"),
        resolve = _.value.cost
      ),
      Field("telephone", OptionType(ListType(StringType)),
        Some("event contact telephone"),
        resolve = _.value.telephone
      ),
      Field("venue", OptionType(ListType(StringType)),
        Some("event venue"),
        resolve = _.value.venue
      ),
      Field("venueCategory", OptionType(ListType(StringType)),
        Some("venue category"),
        resolve = _.value.venueCategory
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
