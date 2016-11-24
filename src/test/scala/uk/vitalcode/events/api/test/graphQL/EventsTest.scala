package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import com.sksamuel.elastic4s.ElasticDsl.{index, _}
import org.scalatest.{Matchers, WordSpec}
import sangria.macros._
import spray.json.{JsNumber, _}
import uk.vitalcode.events.api.models.TokenEntity
import uk.vitalcode.events.api.test.utils.BaseTest

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

  "events" when {
    "authenticated user" when {
      "requesting events with both start and limit parameters" should {
        "return all events" in new Context {
          val user = basicUser(testUsers)
          events(route, 0, 10, userToken(user)) {
            status shouldEqual StatusCodes.OK
            responseAs[JsValue] shouldBe
              """
              {
                "data": {
                  "events": {
                    "total": 4,
                    "items": [{"id": "1"}, {"id": "2"},{"id": "3"}, {"id": "4"}]
                  }
                }
              }""".parseJson
          }
        }
        "return requested page only events" in new Context {
          val user = basicUser(testUsers)
          events(route, 1, 2, userToken(user)) {
            status shouldEqual StatusCodes.OK
            responseAs[JsValue] shouldBe
              """
              {
                "data": {
                  "events": {
                    "total": 4,
                    "items": [{"id": "2"},{"id": "3"}]
                  }
                }
              }""".parseJson
          }
        }
      }
      "requesting events with either start or limit parameter only" should {
        "fail if only start parameter is provided" in new Context {
          val user = basicUser(testUsers)
          val query =
            graphql"""
            {
              events(start: 1) {
                total
                items {
                  id
                }
              }
            }"""
          graphCheck(route, query, userToken(user))(
            status shouldEqual StatusCodes.BadRequest
          )
        }
        "fail if only limit parameter is provided" in new Context {
          val user = basicUser(testUsers)
          val query =
            graphql"""
            {
              events(limit: 10) {
                total
                items {
                  id
                }
              }
            }"""
          graphCheck(route, query, userToken(user))(
            status shouldEqual StatusCodes.BadRequest
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
            }"""
        "return events scheduled after specified date" in new Context {
          val user = basicUser(testUsers)
          graphCheck(route, query, userToken(user),
            vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10), "date" -> JsString("2016-01-08T11:00:00"))
          ) {
            status shouldEqual StatusCodes.OK
            responseAs[JsValue] shouldBe
              """
              {
                "data": {
                  "events": {
                    "total": 2,
                    "items": [{"id": "3"}, {"id": "4"}]
                  }
                }
              }""".parseJson
          }
        }
        "return events containing specified clue" in new Context {
          val user = basicUser(testUsers)
          graphCheck(route, query, userToken(user),
            vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10), "clue" -> JsString("event2"))
          ) {
            status shouldEqual StatusCodes.OK
            responseAs[JsValue] shouldBe
              """
              {
                "data": {
                  "events": {
                    "total": 1,
                    "items": [{"id": "2"}]
                  }
                }
              }""".parseJson
          }
        }
        "return events for specified category" in new Context {
          val user = basicUser(testUsers)
          graphCheck(route, query, userToken(user),
            vars = JsObject("start" → JsNumber(0), "limit" → JsNumber(10), "category" -> JsString("FAMILY"))
          ) {
            status shouldEqual StatusCodes.OK
            responseAs[JsValue] shouldBe
              """
              {
                "data": {
                  "events": {
                    "total": 2,
                    "items": [{"id": "1"}, {"id": "3"}]
                  }
                }
              }""".parseJson
          }
        }
      }
    }
    "not authenticated user" should {
      "fail to return requested events page" in new Context {
        events(route, 0, 10, None) {
          val error = responseAs[GraphqlError]
          status shouldEqual StatusCodes.OK
          error.message shouldBe "Invalid token (SecurityMiddleware)"
          error.path shouldBe "events"
        }
      }
    }
  }

  private def events(route: server.Route, start: Int, limit: Int, token: Option[TokenEntity] = None)(action: => Unit) = {
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
    graphCheck(route, query, token, vars = JsObject("start" → JsNumber(start), "limit" → JsNumber(limit)))(action)
  }
}

