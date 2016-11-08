package uk.vitalcode.events.api.http.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.Credentials
import sangria.execution._
import sangria.marshalling.ResultMarshaller
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue, _}
import uk.vitalcode.events.api.http.{AuthenticationException, AuthorisationException, EventContext, SecurityMiddleware}
import uk.vitalcode.events.api.utils.Config

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class EventsServiceRoute(val eventContext: EventContext)(implicit executionContext: ExecutionContext) extends Config {


  def authenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p @ Credentials.Provided(id) => {
        Some(id)
      }
//      case p @ Credentials.Provided(id) if p.verify("p4ssw0rd") => Some(id)
      //case _ => None
      case _ => Some("test_token")
    }

  val errorHandler: PartialFunction[(ResultMarshaller, Throwable), HandledException] = {
    case (m, AuthenticationException(message)) ⇒ HandledException(message)
    case (m, AuthorisationException(message)) ⇒ HandledException(message)
  }

  val route: Route =
    (post & path("graphql")) {
      authenticateOAuth2("Authorization", authenticator) { token ⇒
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

          //eventContext.setToken(token.replaceAll("""Bearer""", "").trim)
          eventContext.setToken(Some(token))

          QueryParser.parse(query) match {

            // query parsed successfully, time to execute it!
            case Success(queryAst) ⇒
              complete(Executor.execute(SchemaDefinition.EventSchema, queryAst,
                userContext = eventContext,
                operationName = operation,
                variables = vars,
                exceptionHandler = errorHandler,
                middleware = SecurityMiddleware :: Nil)
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
      }
    } ~
      akka.http.scaladsl.server.Directives.get {
        getFromResource("graphiql.html")
      }
}

