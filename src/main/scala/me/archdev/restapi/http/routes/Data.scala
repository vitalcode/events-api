package me.archdev.restapi.http.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.get.GetField
import org.elasticsearch.index.query
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder.Operator
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type
import org.elasticsearch.search.sort.SortOrder
import sangria.schema.{Deferred, DeferredResolver}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.Try

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
                  category: Seq[String],
                  description: Seq[String],
                  from: Seq[LocalDateTime]
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
        friendIds map (id => CharacterRepo.humans.find(_.id == id) orElse CharacterRepo.droids.find(_.id == id))))
  }
}


class CharacterRepo(implicit client: ElasticClient, indexType: IndexType) {

  import CharacterRepo._

  implicit object CharacterHitAs extends HitAs[Event] {
    override def as(hit: RichSearchHit): Event = {
      Event(
        id = hit.id,
        category = hit.sourceAsMap("category").asInstanceOf[java.util.ArrayList[String]].toSeq,
        description = hit.sourceAsMap("description").asInstanceOf[java.util.ArrayList[String]].toSeq,
        from = hit.sourceAsMap("from").asInstanceOf[java.util.ArrayList[String]].map(d => LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      )
    }
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
                category: Option[String],
                start: Int,
                limit: Int,
                fieldSet: String*): Option[Page[Event]] = {

    var mustQuery = Seq.empty[QueryDefinition]
    mustQuery = appendQuery(date, mustQuery, (d: LocalDateTime) => rangeQuery("from") includeLower true from d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    mustQuery = appendQuery(clue, mustQuery, (c: String) => multiMatchQuery(c) fields("description", "title", "venue") operator "and")
    mustQuery = appendQuery(category, mustQuery, (cat: String) => termQuery("category", cat))

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
}

object CharacterRepo {
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

