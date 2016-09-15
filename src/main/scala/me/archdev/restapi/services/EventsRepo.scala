package me.archdev.restapi.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, IndexType}
import org.elasticsearch.index.get.GetField

import scala.collection.JavaConversions._

class EventsRepo(implicit client: ElasticClient, indexType: IndexType) {
    def getEvent(eventId: String, fieldSet: String*): Option[Map[String, Seq[String]]] = {
        val response = client.execute {
            get id eventId from indexType fields fieldSet
        }.await

        if (response.isExists) Some(response.getFields.map {
            case (s: String, f: GetField) => (s, f.getValues.map(v => v.toString))
        }.toMap)
        else None
    }
}