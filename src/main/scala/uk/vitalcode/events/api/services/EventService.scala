package uk.vitalcode.events.api.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import uk.vitalcode.events.api.models.{Event, Page}
import uk.vitalcode.events.api.utils.ElasticServiceSugar
import org.elasticsearch.search.sort.SortOrder
import uk.vitalcode.events.model.Category._

class EventService(val client: ElasticClient, indexType: IndexType) extends ElasticServiceSugar {

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
}
