package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server
import akka.http.scaladsl.unmarshalling.Unmarshaller
import io.circe.generic.auto._
import io.circe.syntax._
import sangria.ast.{AstNode, Document, Value}
import sangria.macros._
import sangria.renderer.QueryRenderer
import spray.json._
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.test.utils.BaseTest
import sangria.marshalling.sprayJson._
import sangria.marshalling.ResultMarshaller
import sangria.marshalling.InputUnmarshaller
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import sangria.marshalling.sprayJson._
import sangria.marshalling.queryAst._
import sangria.marshalling.queryAst._

import scala.util.Success

class AuthTest extends BaseTest {

  trait Context {
    val testUsers = provisionUsersList(2)
    val route = httpService.graphQLRoute.route
  }

  implicit val um: Unmarshaller[HttpEntity, JsObject] = {
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      data.utf8String.parseJson.asJsObject
    }
  }

  "Auth mutations" should {

    "authorize users by login and password and retrieve token" in new Context {
      val testUser = testUsers(1)
      signInUser(testUser, route) {
        responseAs[JsObject].getFields("data").head.asInstanceOf[JsObject]
          .getFields("login8").isEmpty should be
      }
    }

    //    "register users and retrieve token" in new Context {
    //      val testUser = testUsers(0)
    //      signUpUser(testUser, route) {
    //        response.status should be(StatusCodes.Created)
    //      }
    //    }

  }

  private def signUpUser(user: UserEntity, route: server.Route)(action: => Unit) = {
    val requestEntity = HttpEntity(MediaTypes.`application/json`, user.asJson.noSpaces)
    Post("/graphql", requestEntity) ~> route ~> check(action)
  }

  private def signInUser(user: UserEntity, route: server.Route)(action: => Unit) = {
    val query2 =
      graphql"""
        mutation login {
          login (user: "vit" password: "kuz")
        }
        """

//    val Success(query2) = QueryAstMarshallerForType.renderCompact(query2) . parse(query)
//      """
//        query FetchSomeIDQuery($someId: String!) {
//          human(id: $someId) {
//            name
//            appearsIn
//            friends {
//              id
//              name
//            }
//          }
//        }
//      """)
//    //val q = d.asJson

    //val rendered = queryAstInputUnmarshaller.render(query2.asInstanceOf[AstNode])

    val s = QueryRenderer.render(query2, QueryRenderer.Compact).replaceAll("\"", "\\\\\"")

    val query3 = s"""{"query": "$s"}"""

    //val query4 = s"""{"query":"mutation login{login (user: \\"${user.username}\\" password: \\"${user.password}\\")}","variables":"","operationName":"login"}"""
    val requestEntity = HttpEntity(
      MediaTypes.`application/json`,
      query3
    )
    Post("/graphql", requestEntity) ~> route ~> check(action)
  }
}
