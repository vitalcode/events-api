package me.archdev

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexType
import com.sksamuel.elastic4s.testkit.ElasticSugar
import me.archdev.restapi.http.routes.{CharacterRepo, FriendsResolver, SchemaDefinition}
import org.scalatest.{Matchers, WordSpec}
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros._
import sangria.marshalling.sprayJson._
import spray.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class EsSpec extends WordSpec with Matchers with ElasticSugar {

  val indexName = getClass.getSimpleName.toLowerCase
  val `type` = "events"

  implicit val indexType: IndexType = indexName / `type`
  implicit val elasticClient = client

  val event = Seq(
    "url" -> Seq("http://www.visitcambridge.org/whats-on/official-guided-tours-cambridge-college-tour-including-kings-college-p568001"),
    "image" -> Seq("http://www.visitcambridge.org/imageresizer/?image=%2Fdmsimgs%2FGuided%2DTour%2D6%5F68928799%2Ejpg&action=ProductMain"),
    "description" -> Seq(
      "Only our Official Cambridge Guides are permitted to take groups inside the Cambridge Colleges so visit with an expert and don’t just settle for looking at these wonderful buildings from the outside! Our Blue and Green Badge Guides bring the history of Cambridge to life with fun facts and great stories.",
      "Hear about the famous people connected with Cambridge whilst looking at some of the best-known and impressive sights the city has to offer. Entrance to the magnificent King's College and Chapel is included in the ticket price.",
      "What people have said about our tours... We were delighted with our guide who I can personally say was the best guide I have ever had on the numerous tours I have undertaken both here and abroad. She judged her audience well and her sense of humour struck many chords!"
    ),
    "category" -> Seq("family"),
    "to" -> Seq("2016-01-06T13:00:00"),
    "from" -> Seq("2016-01-06T11:00:00"),
    "telephone" -> Seq("Tel: 01223 791501"),
    "title" -> Seq("Official Guided Tours: Cambridge College Tour - including King’s College"),
    "venue" -> Seq("Visitor Information Centre Peas Hill Cambridge Cambs CB2 3AD")
  )

  client.execute {
    bulk(
      index into indexType fields event id 1
    )
  }.await

  blockUntilCount(1, indexName)


  "StartWars Schema" should {
    "correctly identify R2-D2 as the hero of the Star Wars Saga" in {

      val eventsRepo = new CharacterRepo()

      //      val fields: Option[Map[String, Seq[String]]] = eventsRepo.getEvent("1", "category", "description")

      val fields = eventsRepo.getEvent("1", "category", "description")

      //      fields.get.size shouldBe 2
      //      fields.get("category") shouldBe Seq("family")
      //      fields.get("description") shouldBe Seq(
      //        "Only our Official Cambridge Guides are permitted to take groups inside the Cambridge Colleges so visit with an expert and don’t just settle for looking at these wonderful buildings from the outside! Our Blue and Green Badge Guides bring the history of Cambridge to life with fun facts and great stories.",
      //        "Hear about the famous people connected with Cambridge whilst looking at some of the best-known and impressive sights the city has to offer. Entrance to the magnificent King's College and Chapel is included in the ticket price.",
      //        "What people have said about our tours... We were delighted with our guide who I can personally say was the best guide I have ever had on the numerous tours I have undertaken both here and abroad. She judged her audience well and her sense of humour struck many chords!"
      //      )

      fields.get.category shouldBe Seq("family")
      fields.get.description shouldBe Seq(
        "Only our Official Cambridge Guides are permitted to take groups inside the Cambridge Colleges so visit with an expert and don’t just settle for looking at these wonderful buildings from the outside! Our Blue and Green Badge Guides bring the history of Cambridge to life with fun facts and great stories.",
        "Hear about the famous people connected with Cambridge whilst looking at some of the best-known and impressive sights the city has to offer. Entrance to the magnificent King's College and Chapel is included in the ticket price.",
        "What people have said about our tours... We were delighted with our guide who I can personally say was the best guide I have ever had on the numerous tours I have undertaken both here and abroad. She judged her audience well and her sense of humour struck many chords!"
      )
    }
  }

  "correctly identify R2-D2 as the hero of the Star Wars Saga2" in {

    val query =
      graphql"""
          query FetchSomeIDQuery($$humanId: String!) {
            human(id: $$humanId) {
              name
              friends {
                id
                name
              }
            }
          }
          """

    executeQuery(query, vars = JsObject("humanId" → JsString("1002"))) should be(
      """
         {
           "data": {
             "human": {
               "name": "Han Solo",
               "friends": [
                 {
                   "id": "1000",
                   "name": "Luke Skywalker"
                 },
                 {
                   "id": "1003",
                   "name": "Leia Organa"
                 },
                 {
                   "id": "2001",
                   "name": "R2-D2"
                 }
               ]
             }
           }
         }
      """.parseJson)
  }

  "correctly identify R2-D2 as the hero of the Star Wars Saga3" in {

    val query =
      graphql"""
          query FetchSomeIDQuery($$eventId: String!) {
            event(id: $$eventId) {
              category
              description
            }
          }
          """

    executeQuery(query, vars = JsObject("eventId" → JsString("1"))) should be(
      """
         {
           "data": {
             "event": {
               "category": ["family"],
               "description": [
                  "Only our Official Cambridge Guides are permitted to take groups inside the Cambridge Colleges so visit with an expert and don’t just settle for looking at these wonderful buildings from the outside! Our Blue and Green Badge Guides bring the history of Cambridge to life with fun facts and great stories.",
                  "Hear about the famous people connected with Cambridge whilst looking at some of the best-known and impressive sights the city has to offer. Entrance to the magnificent King's College and Chapel is included in the ticket price.",
                  "What people have said about our tours... We were delighted with our guide who I can personally say was the best guide I have ever had on the numerous tours I have undertaken both here and abroad. She judged her audience well and her sense of humour struck many chords!"
               ]
             }
           }
         }
      """.parseJson)
  }

  def executeQuery(query: Document, vars: JsObject = JsObject.empty) = {
    val futureResult = Executor.execute(SchemaDefinition.StarWarsSchema, query,
      variables = vars,
      userContext = new CharacterRepo,
      deferredResolver = new FriendsResolver)

    Await.result(futureResult, 10.seconds)
  }

}
