package me.archdev.restapi.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import me.archdev.restapi.models.{Event, Page}
import org.elasticsearch.search.sort.SortOrder
import uk.vitalcode.events.model.Category
import uk.vitalcode.events.model.Category._

import scala.collection.JavaConversions._


trait EventService {
  val client: ElasticClient
  val indexType: IndexType

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

  def getEvent(eventId: String, fieldSet: String*): Option[Event] = {

    val response = client.execute {
      search in indexType query {
        termQuery("_id", eventId)
      } sourceInclude (fieldSet: _*)
    }.await

    if (!response.isEmpty) Some(response.as[Event].apply(0))
    else None
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

  private def mapElasticFieldValue[T](hit: RichSearchHit, field: String, mapper: String => T): Option[Seq[T]] = {
    val sourceMap = hit.sourceAsMap
    if (sourceMap.isDefinedAt(field)) {
      Some(sourceMap(field).asInstanceOf[java.util.ArrayList[String]].map(mapper))
    }
    else None
  }

  private def appendQuery[T](date: Option[T], must: Seq[QueryDefinition], fn: T => QueryDefinition): Seq[QueryDefinition] = {
    date.map(e => must :+ fn(e)) getOrElse must
  }
}
