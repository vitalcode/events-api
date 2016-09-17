package me.archdev.restapi.http.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexType}
import me.archdev.restapi.http.SecurityDirectives
import me.archdev.restapi.services.{AuthService, UsersService}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue, _}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class GrahpQL(val authService: AuthService,
              usersService: UsersService
             )(implicit executionContext: ExecutionContext) extends SecurityDirectives {

  val route: Route =
    (post & path("graphql")) {
      entity(as[JsValue]) { requestJson ⇒
        val JsObject(fields) = requestJson

        val JsString(query) = fields("query")

        val operation = fields.get("operationName") collect {
          case JsString(op) ⇒ op
        }

        val vars = fields.get("variables") match {
          case Some(obj: JsObject) ⇒ obj
          case Some(JsString(s)) if s.trim.nonEmpty ⇒ s.parseJson
          case _ ⇒ JsObject.empty
        }

        // TODO
        val uri = ElasticsearchClientUri("elasticsearch://localhost:9300")
        implicit val client = ElasticClient.remote(uri)

        implicit val indexType: IndexType = "test_index" / "events"

        QueryParser.parse(query) match {

          // query parsed successfully, time to execute it!
          case Success(queryAst) ⇒
            complete(Executor.execute(SchemaDefinition.EventSchema, queryAst, new EventRepo,
              operationName = operation,
              variables = vars,
              deferredResolver = new FriendsResolver)
              .map(OK → _)
              .recover {
                case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
                case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
              })

          // can't parse GraphQL query, return error
          case Failure(error) ⇒
            complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
        }
      }
    } ~
      akka.http.scaladsl.server.Directives.get {
        getFromResource("graphiql.html")
      }

}