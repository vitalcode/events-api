package me.archdev.graphQL

import me.archdev.utils.ElasticTest
import org.scalatest.{Matchers, WordSpec}
import sangria.macros._
import spray.json._

class EventsTest extends WordSpec with Matchers with ElasticTest {

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

