package me.archdev.graphQL

import me.archdev.utils.ElasticTest
import org.scalatest.{Matchers, WordSpec}
import sangria.macros._
import spray.json._

class EventsTest extends WordSpec with Matchers with ElasticTest {

  "GraphQL: events" when {
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
    "requesting events page with only start and limit parameters" should {
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
  }
}

