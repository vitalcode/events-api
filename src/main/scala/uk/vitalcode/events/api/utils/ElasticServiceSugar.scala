package uk.vitalcode.events.api.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.sksamuel.elastic4s.{HitAs, QueryDefinition, RichSearchHit}
import uk.vitalcode.events.api.models.Event
import uk.vitalcode.events.model.Category

import scala.collection.JavaConversions._

trait ElasticServiceSugar {

  private def timeFormatter(value: String) = {
    LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }

  implicit object CharacterHitAs extends HitAs[Event] {
    override def as(hit: RichSearchHit): Event = {
      Event(
        id = hit.id,
        url = mapElasticFieldValue(hit, "url"),
        title = mapElasticFieldValue(hit, "title"),
        from = mapElasticFieldValue(hit, "from", v => timeFormatter(v)),
        to = mapElasticFieldValue(hit, "to", v => timeFormatter(v)),
        category = mapElasticFieldValue(hit, "category", v => Category.withName(v.toUpperCase)), // TODO Use upper case category in ES
        description = mapElasticFieldValue(hit, "description"),
        image = mapElasticFieldValue(hit, "image"),
        cost = mapElasticFieldValue(hit, "cost"),
        telephone = mapElasticFieldValue(hit, "telephone"),
        venue = mapElasticFieldValue(hit, "venue"),
        venueCategory = mapElasticFieldValue(hit, "venue-category")
      )
    }
  }

  def mapElasticFieldValue(hit: RichSearchHit, field: String): Option[Seq[String]] = {
    mapElasticFieldValue(hit, field, identity[String])
  }

  def mapElasticFieldValue[T](hit: RichSearchHit, field: String, mapper: String => T): Option[Seq[T]] = {
    val sourceMap = hit.sourceAsMap
    if (sourceMap.isDefinedAt(field)) {
      Some(sourceMap(field).asInstanceOf[java.util.ArrayList[String]].map(mapper))
    }
    else None
  }

  def appendQuery[T](date: Option[T], must: Seq[QueryDefinition], fn: T => QueryDefinition): Seq[QueryDefinition] = {
    date.map(e => must :+ fn(e)) getOrElse must
  }
}
