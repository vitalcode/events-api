package me.archdev.graphQL

import com.sksamuel.elastic4s.ElasticDsl.{index, _}
import me.archdev.utils.BaseTest
import org.scalatest.{Matchers, WordSpec}
import sangria.macros._
import spray.json._

class EventTest extends WordSpec with Matchers with BaseTest {

  client.execute {
    bulk(
      index into indexType fields {
        Seq(
          "title" -> Seq("title: event1"),
          "url" -> Seq("http://www.fillyourday.com/event1"),
          "image" -> Seq("http://www.fillyourday.com/event1/image1"),
          "description" -> Seq("line1: event1", "line2: event1", "line3: event1"),
          "category" -> Seq("family"),
          "from" -> Seq("2016-01-06T11:00:00"),
          "to" -> Seq("2016-01-06T13:00:00"),
          "telephone" -> Seq("Tel: 01223 791501"),
          "venue" -> Seq("Venue: event1")
        )
      } id 1,
      index into indexType fields {
        // no description, to
        Seq(
          "title" -> Seq("title: event2"),
          "url" -> Seq("http://www.fillyourday.com/event2"),
          "image" -> Seq("http://www.fillyourday.com/event2/image1"),
          "category" -> Seq("music"),
          "from" -> Seq("2016-01-07T11:00:00"),
          "telephone" -> Seq("Tel: 01223 791502"),
          "venue" -> Seq("Venue: event2")
        )
      } id 2
    )
  }.await

  blockUntilCount(2, indexName)

  "GraphQL: event" should {
    val query =
      graphql"""
          query FetchEvent($$eventId: String!) {
            event(id: $$eventId) {
              category
              description
              from
            }
          }
          """

    "correctly return event when requested using event ID" in {
      executeQuery(query, vars = JsObject("eventId" → JsString("1"))) should be(
        """
         {
           "data": {
             "event": {
               "category": ["FAMILY"],
               "description": ["line1: event1", "line2: event1", "line3: event1"],
               "from": ["2016-01-06T11:00:00"]
             }
           }
         }
        """.parseJson)
    }
    "ignore requested fields if they are not defined in elastic index" in {
      executeQuery(query, vars = JsObject("eventId" → JsString("2"))) should be(
        """
         {
           "data": {
             "event": {
                "category": ["MUSIC"],
                "description": null,
                "from": ["2016-01-07T11:00:00"]
             }
           }
         }
        """.parseJson)
    }
  }
}
