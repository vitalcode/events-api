package me.archdev.utils

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexType
import com.sksamuel.elastic4s.testkit.ElasticSugar
import me.archdev.restapi.http.routes.{EventRepo, SchemaDefinition}
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import spray.json.JsObject

import scala.concurrent.ExecutionContext.Implicits.global

trait ElasticTest extends ElasticSugar {
  val indexName = getClass.getSimpleName.toLowerCase
  val elasticType = "type"

  implicit val indexType: IndexType = indexName / elasticType
  implicit val elasticClient = client

  def executeQuery(query: Document, vars: JsObject = JsObject.empty) = {
    Executor.execute(
      schema = SchemaDefinition.EventSchema,
      queryAst = query,
      variables = vars,
      userContext = new EventRepo
    ).await
  }

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
          "category" -> Seq("family"),
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
          "category" -> Seq("family"),
          "from" -> Seq("2016-01-09T11:00:00"),
          "to" -> Seq("2016-01-09T13:00:00"),
          "telephone" -> Seq("Tel: 01223 791504"),
          "venue" -> Seq("Venue: event4")
        )
      } id 4
    )
  }.await

  blockUntilCount(1, indexName)
}
