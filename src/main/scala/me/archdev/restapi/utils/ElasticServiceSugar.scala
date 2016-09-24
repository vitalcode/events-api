package me.archdev.restapi.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.sksamuel.elastic4s.{HitAs, QueryDefinition, RichSearchHit}
import me.archdev.restapi.models.Event
import uk.vitalcode.events.model.Category

import scala.collection.JavaConversions._

trait ElasticServiceSugar {

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
