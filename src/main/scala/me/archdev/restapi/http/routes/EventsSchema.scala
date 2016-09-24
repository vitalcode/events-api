package me.archdev.restapi.http.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import me.archdev.restapi.http.{Authorised, EventContext}
import me.archdev.restapi.models.{Event, Page}
import sangria.ast
import sangria.schema._
import sangria.validation.ValueCoercionViolation
import uk.vitalcode.events.model.Category
import uk.vitalcode.events.model.Category.Category

import scala.util.{Failure, Success, Try}


/**
  * Defines a GraphQL schema for the current project
  */
object SchemaDefinition {

  case object DateCoercionViolation extends ValueCoercionViolation("Date value expected")

  def parseDate(s: String) = Try(LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)) match {
    case Success(date) => Right(date)
    case Failure(_) => Left(DateCoercionViolation)
  }

  val EventCategoryEnum = EnumType[Category](
    "EventCategory",
    Some("Event category"),
    Category.values.toList.map(category =>
      EnumValue(category.toString,
        value = category,
        description = Some(s"${category.toString} event category")))
  )


  val DateType = ScalarType[LocalDateTime]("Date",
    description = Some("An example of date scalar type"),
    coerceOutput = (d, _) ⇒ d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    coerceUserInput = {
      case s: String ⇒ parseDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _) ⇒ parseDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    })

  val Event = ObjectType(
    "Event",
    "Just Event",
    fields = fields[Unit, Event](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id),
      Field("category", OptionType(ListType(EventCategoryEnum)),
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

  val Page = ObjectType(
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


  val ID = Argument("id", StringType, description = "id of the character")
  val Date = Argument("date", OptionInputType(DateType), description = "event search date")
  val Clue = Argument("clue", OptionInputType(StringType), description = "event search clue")
  val CategoryArg = Argument("category", OptionInputType(EventCategoryEnum), description = "event search category")
  val Start = Argument("start", IntType, description = "event list start")
  val Limit = Argument("limit", IntType, description = "event list limit")

  val QueryType = ObjectType("Query", fields[EventContext, Unit](
    Field("event", Event,
      arguments = ID :: Nil,
      tags = Authorised :: Nil,
      resolve = ctx => ctx.ctx.getEvent(ctx arg ID).get),
    Field("events", Page,
      arguments = Date :: Clue :: CategoryArg :: Start :: Limit :: Nil,
      resolve = ctx => ctx.ctx.getEvents(
        date = ctx.arg(Date),
        clue = ctx.arg(Clue),
        category = ctx.arg(CategoryArg),
        start = ctx.arg(Start),
        limit = ctx.arg(Limit)).get)
  ))

  val UserNameArg = Argument("user", StringType)
  val PasswordArg = Argument("password", StringType)

  val MutationType = ObjectType("Mutation", fields[EventContext, Unit](
    Field("login", OptionType(StringType),
      arguments = UserNameArg :: PasswordArg :: Nil,
      resolve = ctx ⇒ UpdateCtx(ctx.ctx.login(ctx.arg(UserNameArg), ctx.arg(PasswordArg)).token) { token ⇒
        ctx.ctx.setToken(Some(token))
        ctx.ctx // todo copy no mutation
//        ctx.ctx.copy(token = Some(token.token))
      })
    //    Field("addColor", OptionType(ListType(StringType)),
    //      arguments = ColorArg :: Nil,
    //      tags = Permission("EDIT_COLORS") :: Nil,
    //      resolve = ctx ⇒ {
    //        ctx.ctx.colorRepo.addColor(ctx.arg(ColorArg))
    //        ctx.ctx.colorRepo.colors
    //      })
  ))

  val EventSchema = Schema(QueryType, Some(MutationType))
}
