package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.server
import akka.http.scaladsl.unmarshalling.Unmarshaller
import io.circe.generic.auto._
import io.circe.syntax._
import sangria.macros._
import spray.json.{JsString, _}
import uk.vitalcode.events.api.models.{TokenEntity, UserEntity}
import uk.vitalcode.events.api.test.utils.BaseTest

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random

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
        val token = Await.result(authService.signIn(testUser.username, testUser.password), Duration.Inf)
        token.isDefined shouldBe true
        responseAs[JsObject] shouldBe responseToken(token.get.token)
      }
    }

    "register users and retrieve token" in new Context {
      val testUser = provisionUser
      signUpUser(testUser, route) {
        val token = Await.result(authService.signIn(testUser.username, testUser.password), Duration.Inf)
        responseAs[JsObject] shouldBe responseToken(token.get.token)
      }
    }
  }

  private def responseToken (token: String) = JsObject("data" -> JsObject("token" -> JsString(token)))

  private def signUpUser(user: UserEntity, route: server.Route)(action: => Unit) = {
    val query =
      graphql"""
        mutation signUp($$user: String! $$password: String!){
          token: signUp (user: $$user password: $$password)
        }
        """
    val requestEntity = HttpEntity(MediaTypes.`application/json`,
      graphRequest(query, vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password)))
    )
    Post("/graphql", requestEntity) ~> route ~> check(action)
  }

  private def signInUser(user: UserEntity, route: server.Route)(action: => Unit) = {
    val query =
      graphql"""
        mutation logIn($$user: String! $$password: String!){
          token: logIn (user: $$user password: $$password)
        }
        """
    val requestEntity = HttpEntity(MediaTypes.`application/json`,
      graphRequest(query, vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password)))
    )
    Post("/graphql", requestEntity) ~> route ~> check(action)
  }
}
