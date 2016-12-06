package uk.vitalcode.events.api.test.graphQL

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import sangria.macros._
import spray.json.{JsString, _}
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.test.utils.BaseTest
import uk.vitalcode.events.api.utils.JwtUtils

class AuthTest extends BaseTest {

  // TODO Test for user stories, e.g. user register -> login -> me (multiple requests)

  "authentication" when {
    "register" when {
      "admin user" should {
        "register new user and retrieve its token" in new Context {
          val subject = adminUser(testUsers)
          val newUser = createTestUser()
          registerUser(route, newUser, Some(subject)) {
            val userFromToken = tokenResponseToUser(responseAs[JsValue])
            status shouldEqual StatusCodes.OK // TODO Check if the user has beed added to the database
            userFromToken.username shouldBe newUser.username // TODO refactor
          }
        }
      }
      "not admin user" should {
        "fail to register new user" in new Context {
          val subject = basicUser(testUsers)
          val newUser = createTestUser()
          registerUser(route, newUser, Some(subject)) {
            status shouldEqual StatusCodes.OK
            responseAs[JsValue] shouldBe {
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
          status shouldEqual StatusCodes.OK
          tokenResponseToUser(responseAs[JsValue]) shouldBe user
        }
      }
    }
    "users" should {
      "return all registered users" in new Context {
        val subject = basicUser(testUsers)
        users(route, Some(subject)) {
          status shouldEqual StatusCodes.OK
          responseAs[JsValue] shouldBe
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
        val subject = basicUser(testUsers)
        me(route, Some(subject)) {
          status shouldEqual StatusCodes.OK
          responseAs[JsValue] shouldBe
            s"""
          {
            "data": {
              "me": {
                "id": ${subject.id.get},
                "username": "${subject.username}"
              }
            }
          }""".parseJson
        }
      }
      "fail to get user information for unauthorized user" in new Context {
        me(route) {
          status shouldEqual StatusCodes.OK
          responseAs[JsValue] shouldBe
            s"""
          {
            "data": {
              "me": null
            },
            "errors": [{
              "message": "Invalid token",
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

  private def registerUser(route: server.Route, user: UserEntity, subject: Option[UserEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
        mutation register ($$user: String ! $$password: String !)
        {
          token: register (user: $$user password: $$password)
        }
        """
    graphCheck(route, query, subject,
      vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password))
    )(action)
  }

  private def me(route: server.Route, subject: Option[UserEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
      {
        me {
          id,
          username
        }
      }"""
    graphCheck(route, query, subject)(action)
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

  private def users(route: server.Route, subject: Option[UserEntity] = None)(action: => Unit) = {
    val query =
      graphql"""
      {
        users {
          id,
          username
        }
      }"""
    graphCheck(route, query, subject)(action)
  }

  private def tokenResponseToUser(response: JsValue) = {
    val token = response.asInstanceOf[JsObject].fields("data").asInstanceOf[JsObject].fields("token").asInstanceOf[JsString].value
    JwtUtils.decodeSubject(token).get
  }
}
