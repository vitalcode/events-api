package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server
import akka.http.scaladsl.unmarshalling.Unmarshaller
import sangria.macros._
import spray.json.{JsString, _}
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.test.utils.BaseTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

  "Auth" when {
    "register" should {
      "register user and retrieve token" in new Context {
        val user = testUser
        registerUser(route, user) {
          val token = Await.result(authService.login(user.username, user.password), Duration.Inf)
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe tokenResponse(token.get.token)
        }
      }
    }
    "login" should {
      "create and return new token for the user with correct login credentials" in new Context {
        val user = testUsers(1)
        login(route, user) {
          val token = Await.result(authService.tokenByUser(user), Duration.Inf)
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe tokenResponse(token.get.token)
        }
      }
    }
//    "logout" should {
//      "remove user login from the database"
//
//    }
//    "users" should {
//      "return all registered users"
//    }
    "me" should {
      "get user information for authorized user" in new Context {
        val testUser = testUsers(1)
        me(route, Some(testUser)) {
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe
            s"""
          {
            "data": {
              "me": {
                "id": ${testUser.id.get},
                "username": "${testUser.username}"
              }
            }
          }""".parseJson
        }
      }
      "fail to get user information for unauthorized user" in new Context {
        me(route) {
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe
            s"""
          {
            "data": {
              "me": null
            },
            "errors": [{
              "message": "Invalid token (SecurityMiddleware)",
              "path": ["me"],
              "locations": [{
                "line": 1,
                "column": 2
              }]
            }]
          }""".parseJson
        }
      }
    }
  }

  private def registerUser(route: server.Route, user: UserEntity)(action: => Unit) = {
    val query =
      graphql"""
        mutation register ($$user: String ! $$password: String !)
        {
          token: register (user: $$user password: $$password)
        }
        """
    graphCheck(route, query,
      vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password))
    )(action)
  }

  private def me(route: server.Route, user: Option[UserEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
      {
        me {
          id,
          username
        }
      }"""
    graphCheck(route, query, user)(action)
  }

  private def login(route: server.Route, user: UserEntity)(action: => Unit) = {
    val query =
      graphql"""
        mutation login ($$user: String ! $$password: String !)
        {
          token: login (user: $$user password: $$password)
        }
        """
    graphCheck(route, query,
      vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password))
    )(action)
  }

  private def tokenResponse(token: String) = {
    s"""
    {
      "data": {
        "token": "${token}"
      }
    }""".parseJson
  }
}
