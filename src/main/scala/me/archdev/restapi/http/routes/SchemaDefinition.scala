package me.archdev.restapi.http.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import sangria.marshalling.DateSupport
import sangria.schema._
import sangria.validation.ValueCoercionViolation
import sangria.ast

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import uk.vitalcode.events.model.Category
import uk.vitalcode.events.model.Category.Category

/**
  * Defines a GraphQL schema for the current project
  */
object SchemaDefinition {
  val EpisodeEnum = EnumType(
    "Episode",
    Some("One of the films in the Star Wars Trilogy"),
    List(
      EnumValue("NEWHOPE",
        value = Episode.NEWHOPE,
        description = Some("Released in 1977.")),
      EnumValue("EMPIRE",
        value = Episode.EMPIRE,
        description = Some("Released in 1980.")),
      EnumValue("JEDI",
        value = Episode.JEDI,
        description = Some("Released in 1983."))))

  val Character: InterfaceType[Unit, Character] =
    InterfaceType(
      "Character",
      "A character in the Star Wars Trilogy",
      () => fields[Unit, Character](
        Field("id", StringType,
          Some("The id of the character."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the character."),
          resolve = _.value.name),
        Field("friends", OptionType(ListType(OptionType(Character))),
          Some("The friends of the character, or an empty list if they have none."),
          resolve = ctx => DeferFriends(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e => Some(e)))
      ))

  val Human =
    ObjectType(
      "Human",
      "A humanoid creature in the Star Wars universe.",
      interfaces[Unit, Human](Character),
      fields[Unit, Human](
        Field("id", StringType,
          Some("The id of the human."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the human."),
          resolve = _.value.name),
        Field("friends", OptionType(ListType(OptionType(Character))),
          Some("The friends of the human, or an empty list if they have none."),
          resolve = (ctx) => DeferFriends(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e => Some(e))),
        Field("homePlanet", OptionType(StringType),
          Some("The home planet of the human, or null if unknown."),
          resolve = _.value.homePlanet)
      ))

  val Droid = ObjectType(
    "Droid",
    "A mechanical creature in the Star Wars universe.",
    interfaces[Unit, Droid](Character),
    fields[Unit, Droid](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id),
      Field("name", OptionType(StringType),
        Some("The name of the droid."),
        resolve = ctx => Future.successful(ctx.value.name)),
      Field("friends", OptionType(ListType(OptionType(Character))),
        Some("The friends of the droid, or an empty list if they have none."),
        resolve = ctx => DeferFriends(ctx.value.friends)),
      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
        Some("Which movies they appear in."),
        resolve = _.value.appearsIn map (e => Some(e))),
      Field("primaryFunction", OptionType(StringType),
        Some("The primary function of the droid."),
        resolve = _.value.primaryFunction)
    ))


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

  val EpisodeArg = Argument("episode", OptionInputType(EpisodeEnum),
    description = "If omitted, returns the hero of the whole saga. If provided, returns the hero of that particular episode.")

  val Query = ObjectType(
    "Query", fields[EventRepo, Unit](
      Field("hero", Character,
        arguments = EpisodeArg :: Nil,
        resolve = (ctx) => ctx.ctx.getHero(ctx.arg(EpisodeArg))),
      Field("human", OptionType(Human),
        arguments = ID :: Nil,
        resolve = ctx => ctx.ctx.getHuman(ctx arg ID)),
      Field("droid", Droid,
        arguments = ID :: Nil,
        resolve = Projector((ctx, f) => ctx.ctx.getDroid(ctx arg ID).get)),
      Field("event", Event,
        arguments = ID :: Nil,
        resolve = ctx => ctx.ctx.getEvent(ctx arg ID).get),
      Field("events", Page,
        arguments = Date :: Clue :: CategoryArg :: Start :: Limit :: Nil,
        resolve = ctx => ctx.ctx.getEvents(
          date = ctx.arg(Date),
          clue = ctx.arg(Clue),
          category = ctx.arg(CategoryArg),
          start = ctx.arg(Start),
          limit = ctx.arg(Limit)).get)
    )
  )

  val EventSchema = Schema(Query)
}
