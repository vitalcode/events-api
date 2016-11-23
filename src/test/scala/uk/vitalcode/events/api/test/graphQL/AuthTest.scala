package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server
import akka.http.scaladsl.unmarshalling.Unmarshaller
import sangria.macros._
import spray.json.{JsString, _}
import uk.vitalcode.events.api.models.{TokenEntity, UserEntity, UserPermission}
import uk.vitalcode.events.api.test.utils.BaseTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AuthTest extends BaseTest {

  trait Context {
    val testUsers = dbTestUsers(2)
    val route = httpService.graphQLRoute.route
  }

  implicit val um: Unmarshaller[HttpEntity, JsObject] = {
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      data.utf8String.parseJson.asJsObject
    }
  }

  "Auth" when {
    "register" when {
      "admin user" should {
        "register new user and retrieve its token" in new Context {
          val admin = adminUser(testUsers)
          val newUser = createTestUser()
          registerUser(route, newUser, userToken(admin)) {
            status shouldEqual StatusCodes.OK
            responseAs[JsObject] shouldBe tokenResponse(userToken(newUser).get.token)
          }
        }
      }
      "not admin user" should {
        "fail to register new user" in new Context {
          val user = basicUser(testUsers)
          val newUser = createTestUser()
          registerUser(route, newUser, userToken(user)) {
            status shouldEqual StatusCodes.OK
            responseAs[JsObject] shouldBe {
              """
              {
                "data": {
                  "token": null
                },
                "errors": [{
                  "message": "You do not have permission to perform this operation",
                  "path": ["token"],
                  "locations": [{
                    "line": 1,
                    "column": 52
                  }]
                }]
              }""".parseJson
            }
          }
        }
      }
    }
    "login" should {
      "create and return new token for the user with correct login credentials" in new Context {
        val user = basicUser(testUsers)
        login(route, user) {
          val token = Await.result(authService.tokenByUser(user), Duration.Inf)
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe tokenResponse(token.get.token)
        }
      }
    }
    "logout" should {
      "remove user token from the database" in new Context {
        val user = basicUser(testUsers)
        val token = Await.result(authService.login(user), Duration.Inf)
        logout(route, token) {
          val tokenAfterLogout = Await.result(authService.tokenByUser(user), Duration.Inf)
          tokenAfterLogout shouldBe None
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe
            """
            {
              "data":{
                "logout":"ok"
              }
            }
            """.parseJson
        }
      }
    }
    "users" should {
      "return all registered users" in new Context {
        val user = basicUser(testUsers)
        val token = Await.result(authService.login(user), Duration.Inf)
        users(route, token) {
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe
            JsObject("data" ->
              JsObject("users" -> JsArray(testUsers.sortBy(u => u.id).map(u => JsObject(
                "id" -> JsNumber(u.id.get),
                "username" -> JsString(u.username)
              )).toVector))
            )
        }
      }
    }
    "me" should {
      "get user information for authorized user" in new Context {
        val user = basicUser(testUsers)
        val token = Await.result(authService.login(user), Duration.Inf)
        me(route, token) {
          status shouldEqual StatusCodes.OK
          responseAs[JsObject] shouldBe
            s"""
          {
            "data": {
              "me": {
                "id": ${user.id.get},
                "username": "${user.username}"
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

  private def registerUser(route: server.Route, user: UserEntity, token: Option[TokenEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
        mutation register ($$user: String ! $$password: String !)
        {
          token: register (user: $$user password: $$password)
        }
        """
    graphCheck(route, query, token,
      vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password))
    )(action)
  }

  private def me(route: server.Route, token: Option[TokenEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
      {
        me {
          id,
          username
        }
      }"""
    graphCheck(route, query, token)(action)
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

  private def logout(route: server.Route, token: Option[TokenEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
        mutation
        {
          logout
        }
        """
    graphCheck(route, query, token)(action)
  }

  private def users(route: server.Route, token: Option[TokenEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
      {
        users {
          id,
          username
        }
      }"""
    graphCheck(route, query, token)(action)
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
