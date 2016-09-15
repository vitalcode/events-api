package me.archdev.restapi.http.routes

import sangria.schema._

import scala.concurrent.Future

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


  val Event = ObjectType(
    "Event",
    "Just Event",
    fields = fields[Unit, Event](
      Field("id", StringType,
        Some("The id of the droid."),
        tags = ProjectionName("_id") :: Nil,
        resolve = _.value.id),
      Field("category", OptionType(ListType(StringType)),
        Some("event category"),
        resolve = _.value.category),
      Field("description", OptionType(ListType(StringType)),
        Some("event description"),
        resolve = _.value.description
      )
    )
  )

  val ID = Argument("id", StringType, description = "id of the character")

  val EpisodeArg = Argument("episode", OptionInputType(EpisodeEnum),
    description = "If omitted, returns the hero of the whole saga. If provided, returns the hero of that particular episode.")

  val Query = ObjectType(
    "Query", fields[CharacterRepo, Unit](
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
      Field("events", OptionType(ListType(Event)),
        resolve = ctx => ctx.ctx.getEvents().get)
    )
  )

  val StarWarsSchema = Schema(Query)
}
