package me.archdev.restapi.http.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import org.elasticsearch.search.sort.SortOrder
import sangria.schema.{Deferred, DeferredResolver}
import uk.vitalcode.events.model.Category
import uk.vitalcode.events.model.Category.Category

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.Try

case class User(userName: String, permissions: List[String])

class UserRepo {
  var tokens = Map.empty[String, User]

  tokens = tokens + ("123456" -> User("Vit", List.empty[String]))

  /** Gives back a token or sessionId or anything else that identifies the user session  */
  def authenticate(userName: String, password: String): Option[String] =
    if (userName == "admin" && password == "secret") {
      val token = UUID.randomUUID().toString
      tokens = tokens + (token → User("admin", "VIEW_PERMISSIONS" :: "EDIT_COLORS" :: "VIEW_COLORS" :: Nil))
      Some(token)
    } else if (userName == "john" && password == "apples") {
      val token = UUID.randomUUID().toString
      tokens = tokens + (token → User("john", "VIEW_COLORS" :: Nil))
      Some(token)
    } else None

  /** Gives `User` object with his/her permissions */
  def authorise(token: String): Option[User] = {
    tokens.get(token)
  }
}

case class AuthenticationException(message: String) extends Exception(message)

case class AuthorisationException(message: String) extends Exception(message)

class ColorRepo {
  var colors = List("red", "green", "blue")

  def addColor(color: String) =
    colors = colors :+ color
}

object Episode extends Enumeration {
  val NEWHOPE, EMPIRE, JEDI = Value
}

trait Character {
  def id: String

  def name: Option[String]

  def friends: List[String]

  def appearsIn: List[Episode.Value]
}

case class Human(
                  id: String,
                  name: Option[String],
                  friends: List[String],
                  appearsIn: List[Episode.Value],
                  homePlanet: Option[String]) extends Character

case class Droid(
                  id: String,
                  name: Option[String],
                  friends: List[String],
                  appearsIn: List[Episode.Value],
                  primaryFunction: Option[String]) extends Character

case class Event(
                  id: String,
                  category: Option[Seq[Category]],
                  description: Option[Seq[String]],
                  from: Option[Seq[LocalDateTime]]
                )

case class Page[T](
                    total: Int,
                    items: Seq[T]
                  )

/**
  * Instructs sangria to postpone the expansion of the friends list to the last responsible moment and then batch
  * all collected defers together.
  */
case class DeferFriends(friends: List[String]) extends Deferred[List[Option[Character]]]

/**
  * Resolves the lists of friends collected during the query execution.
  * For this demonstration the implementation is pretty simplistic, but in real-world scenario you
  * probably want to batch all of the deferred values in one efficient fetch.
  */
class FriendsResolver extends DeferredResolver[Any] {
  override def resolve(deferred: Vector[Deferred[Any]], ctx: Any) = deferred map {
    case DeferFriends(friendIds) =>
      Future.fromTry(Try(
        friendIds map (id => EventRepo.humans.find(_.id == id) orElse EventRepo.droids.find(_.id == id))))
  }
}

class EventRepo(userRepo: UserRepo, colorRepo: ColorRepo)(implicit client: ElasticClient, indexType: IndexType) {

  import EventRepo._

  var token: Option[String] = None

  def setToken(t: Option[String]) = this.token = t

  implicit object CharacterHitAs extends HitAs[Event] {
    override def as(hit: RichSearchHit): Event = {
      Event(
        id = hit.id,
        category = mapElasticFieldValue(hit, "category", v => Category.withName(v.toUpperCase)), // TODO Use upper case category in ES
        description = mapElasticFieldValue(hit, "description", v => v),
        from = mapElasticFieldValue(hit, "from", v => LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      )
    }
  }

  private def mapElasticFieldValue[T](hit: RichSearchHit, field: String, mapper: String => T): Option[Seq[T]] = {
    val sourceMap = hit.sourceAsMap
    if (sourceMap.isDefinedAt(field)) {
      Some(sourceMap(field).asInstanceOf[java.util.ArrayList[String]].map(mapper))
    }
    else None
  }

  def getHero(episode: Option[Episode.Value]) =
    episode flatMap (_ => getHuman("1000")) getOrElse droids.last

  def getHuman(id: String): Option[Human] = humans.find(c => c.id == id)

  def getDroid(id: String): Option[Droid] = droids.find(c => c.id == id)

  def getEvent(eventId: String, fieldSet: String*): Option[Event] = {

    val response = client.execute {
      search in indexType query {
        termQuery("_id", eventId)
      } sourceInclude (fieldSet: _*)
    }.await

    if (!response.isEmpty) Some(response.as[Event].apply(0))
    else None
  }


  def appendQuery[T](date: Option[T], must: Seq[QueryDefinition], fn: T => QueryDefinition): Seq[QueryDefinition] = {
    date.map(e => must :+ fn(e)) getOrElse must
  }

  def getEvents(date: Option[LocalDateTime],
                clue: Option[String],
                category: Option[Category],
                start: Int,
                limit: Int,
                fieldSet: String*): Option[Page[Event]] = {

    var mustQuery = Seq.empty[QueryDefinition]
    mustQuery = appendQuery(date, mustQuery, (d: LocalDateTime) => rangeQuery("from") includeLower true from d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    mustQuery = appendQuery(clue, mustQuery, (c: String) => multiMatchQuery(c) fields("description", "title", "venue") operator "and")
    mustQuery = appendQuery(category, mustQuery, (cat: Category) => termQuery("category", cat.toString.toLowerCase)) // TODO Use upper case category in ES

    val response = client.execute {
      search in indexType query {
        must(mustQuery: _*)
      } sort {
        field sort "from" order SortOrder.ASC mode MultiMode.Min
      } start start limit limit sourceInclude (fieldSet: _*) sort()
    }.await

    if (!response.isEmpty) Some(Page(response.totalHits.toInt, response.as[Event]))
    else None
  }


  def login(userName: String, password: String) = userRepo.authenticate(userName, password) getOrElse (
    throw new AuthenticationException("UserName or password is incorrect"))

  def authorised[T](permissions: String*)(fn: User ⇒ T) =
    token.flatMap(userRepo.authorise).fold(throw AuthorisationException("Invalid token (authorised)")) { user ⇒
      if (permissions.forall(user.permissions.contains)) fn(user)
      else throw AuthorisationException("You do not have permission to do this operation")
    }

  def ensurePermissions(permissions: List[String]): Unit =
    token.flatMap(userRepo.authorise).fold(throw AuthorisationException("Invalid token (ensurePermissions)")) { user ⇒
      if (!permissions.forall(user.permissions.contains))
        throw AuthorisationException("You do not have permission to do this operation")
    }

  def user = token.flatMap(userRepo.authorise).fold(throw AuthorisationException("Invalid token (user)"))(identity)
}

object EventRepo {
  val humans = List(
    Human(
      id = "1000",
      name = Some("Luke Skywalker"),
      friends = List("1002", "1003", "2000", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = Some("Tatooine")),
    Human(
      id = "1001",
      name = Some("Darth Vader"),
      friends = List("1004"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = Some("Tatooine")),
    Human(
      id = "1002",
      name = Some("Han Solo"),
      friends = List("1000", "1003", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = None),
    Human(
      id = "1003",
      name = Some("Leia Organa"),
      friends = List("1000", "1002", "2000", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = Some("Alderaan")),
    Human(
      id = "1004",
      name = Some("Wilhuff Tarkin"),
      friends = List("1001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = None)
  )

  val droids = List(
    Droid(
      id = "2000",
      name = Some("C-3PO"),
      friends = List("1000", "1002", "1003", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      primaryFunction = Some("Protocol")),
    Droid(
      id = "2001",
      name = Some("R2-D2"),
      friends = List("1000", "1002", "1003"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      primaryFunction = Some("Astromech"))
  )
}

