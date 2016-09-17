package me.archdev.graphQL

import me.archdev.utils.ElasticTest
import org.scalatest.{Matchers, WordSpec}
import sangria.macros._
import spray.json._

class EventTest extends WordSpec with Matchers with ElasticTest {

  "GraphQL: event" should {
    "correctly return event when requested using event ID" in {
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

      executeQuery(query, vars = JsObject("eventId" → JsString("1"))) should be(
        """
         {
           "data": {
             "event": {
               "category": ["family"],
               "description": ["line1: event1", "line2: event1", "line3: event1"],
               "from": ["2016-01-06T11:00:00"]
             }
           }
         }
        """.parseJson)
    }
  }
}
