package me.archdev.graphQL

import com.sksamuel.elastic4s.ElasticDsl.{index, _}
import me.archdev.utils.BaseTest
import org.scalatest.{Matchers, WordSpec}
import sangria.macros._
import spray.json._

class EventsTest extends WordSpec with Matchers with BaseTest {

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
        Seq(
          "title" -> Seq("title: event2"),
          "url" -> Seq("http://www.fillyourday.com/event2"),
          "image" -> Seq("http://www.fillyourday.com/event2/image1"),
          "description" -> Seq("line1: event2", "line2: event2", "line3: event2"),
          "category" -> Seq("music"),
          "from" -> Seq("2016-01-07T11:00:00"),
          "to" -> Seq("2016-01-07T13:00:00"),
          "telephone" -> Seq("Tel: 01223 791502"),
          "venue" -> Seq("Venue: event2")
        )
      } id 2,
      index into indexType fields {
        Seq(
          "title" -> Seq("title: event3"),
          "url" -> Seq("http://www.fillyourday.com/event3"),
          "image" -> Seq("http://www.fillyourday.com/event3/image1"),
          "description" -> Seq("line1: event3", "line2: event3", "line3: event3"),
          "category" -> Seq("family"),
          "from" -> Seq("2016-01-08T11:00:00"),
          "to" -> Seq("2016-01-08T13:00:00"),
          "telephone" -> Seq("Tel: 01223 791503"),
          "venue" -> Seq("Venue: event3")
        )
      } id 3,
      index into indexType fields {
        Seq(
          "title" -> Seq("title: event4"),
          "url" -> Seq("http://www.fillyourday.com/event4"),
          "image" -> Seq("http://www.fillyourday.com/event4/image1"),
          "description" -> Seq("line1: event4", "line2: event4", "line3: event4"),
          "category" -> Seq("sport"),
          "from" -> Seq("2016-01-09T11:00:00"),
          "to" -> Seq("2016-01-09T13:00:00"),
          "telephone" -> Seq("Tel: 01223 791504"),
          "venue" -> Seq("Venue: event4")
        )
      } id 4
    )
  }.await

  blockUntilCount(4, indexName)

  "GraphQL: events" when {

    "requesting events page with only start and limit parameters" should {
      val query =
        graphql"""
        query FetchEvents($$start: Int!, $$limit: Int!) {
          events(start: $$start, limit: $$limit) {
            total
            items {
              id
            }
          }
        }
        """
      "return all ordered page events" in {
        executeQuery(query, vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10))) should be(
          """
          {
            "data": {
              "events": {
                "total": 4,
                "items": [{"id": "1"}, {"id": "2"},{"id": "3"}, {"id": "4"}]
              }
            }
          }
          """.parseJson
        )
      }
      "return requested page only events" in {
        executeQuery(query, vars = JsObject("start" → JsNumber(1), "limit" → JsNumber(2))) should be(
          """
          {
            "data": {
              "events": {
                "total": 4,
                "items": [{"id": "2"},{"id": "3"}]
              }
            }
          }
          """.parseJson
        )
      }
    }

    "requesting events using date, clue and category filter" should {
      val query =
        graphql"""
        query FetchEvents($$date: Date, $$clue: String, $$category: EventCategory, $$start: Int!, $$limit: Int!) {
          events(date: $$date, clue: $$clue, category: $$category, start: $$start, limit: $$limit) {
            total
            items {
              id
            }
          }
        }
        """
      "return events scheduled after specified date" in {
        executeQuery(query, vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10),
          "date" -> JsString("2016-01-08T11:00:00")
        )) should be(
          """
          {
            "data": {
              "events": {
                "total": 2,
                "items": [{"id": "3"}, {"id": "4"}]
              }
            }
          }
          """.parseJson
        )
      }
      "return events containing specified clue" in {
        executeQuery(query, vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10),
          "clue" -> JsString("event2")
        )) should be(
          """
          {
            "data": {
              "events": {
                "total": 1,
                "items": [{"id": "2"}]
              }
            }
          }
          """.parseJson
        )
      }
      "return events for specified category" in {
        executeQuery(query, vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10),
          "category" -> JsString("FAMILY")
        )) should be(
          """
          {
            "data": {
              "events": {
                "total": 2,
                "items": [{"id": "1"}, {"id": "3"}]
              }
            }
          }
          """.parseJson
        )
      }
    }
  }
}

