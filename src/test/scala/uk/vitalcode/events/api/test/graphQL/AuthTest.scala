package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server
import akka.http.scaladsl.unmarshalling.Unmarshaller
import io.circe.generic.auto._
import io.circe.syntax._
import sangria.macros._
import spray.json._
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.test.utils.BaseTest

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
          .getFields("login").head.asInstanceOf[JsString].value.length shouldBe 32
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
    val query =
      graphql"""
        mutation login($$user: String! $$password: String!){
          login (user: $$user password: $$password)
        }
        """
    val requestEntity = HttpEntity(
      MediaTypes.`application/json`,
      graphRequest(query, Map("user" -> user.username, "password" -> user.password))
    )
    Post("/graphql", requestEntity) ~> route ~> check(action)
  }
}
