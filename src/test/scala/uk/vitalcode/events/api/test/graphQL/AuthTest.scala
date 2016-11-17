package uk.vitalcode.events.api.test.graphQL

import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, MediaTypes, StatusCodes}
import akka.http.scaladsl.server
import akka.http.scaladsl.unmarshalling.Unmarshaller
import sangria.macros._
import spray.json._
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

  "Auth mutations" should {
    "register users and retrieve token" in new Context {
      val testUser = provisionUser
      signUpUser(testUser, route) {
        val token = Await.result(authService.signIn(testUser.username, testUser.password), Duration.Inf)
        responseAs[JsObject] shouldBe responseToken(token.get.token)
      }
    }

    "authorize users by login and password and retrieve token" in new Context {
      val testUser = testUsers(1)
      signInUser(testUser, route) {
        val token = Await.result(authService.signIn(testUser.username, testUser.password), Duration.Inf)
        token.isDefined shouldBe true
        responseAs[JsObject] shouldBe responseToken(token.get.token)
      }
    }

    "get user information (me) for authorized user" in new Context {
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

    "fail to get user information (me) for unauthorized user" in new Context {
      me(route) {
        status shouldEqual StatusCodes.OK
        val e = responseAs[JsObject]
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


  private def responseToken(token: String) = JsObject("data" -> JsObject("token" -> JsString(token)))

  private def addAuthorizationHeader(request: HttpRequest, user: UserEntity): HttpRequest = {
    val token = Await.result(authService.signIn(user.username, user.password), Duration.Inf)
    request.addHeader(Authorization(HttpCredentials.createOAuth2BearerToken(token.get.token)))
  }

  private def me(route: server.Route, user: Option[UserEntity] = None)(action: => Unit) = {
    val query =
      graphql""" {
            me {
              id,
              username
            }
          }
        """
    val requestEntity = HttpEntity(MediaTypes.`application/json`,
      graphRequest(query)
    )
    val request = Post("/graphql", requestEntity)
    user.map(addAuthorizationHeader(request, _)).getOrElse(request) ~> route ~> check(action)
  }

  private def signUpUser(user: UserEntity, route: server.Route)(action: => Unit) = {
    val query =
      graphql"""
        mutation signUp ($$user: String ! $$password: String !)
        {
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
        mutation logIn ($$user: String ! $$password: String !)
        {
          token: logIn (user: $$user password: $$password)
        }
        """
    val requestEntity = HttpEntity(MediaTypes.`application/json`,
      graphRequest(query, vars = JsObject("user" → JsString(user.username), "password" → JsString(user.password)))
    )
    Post("/graphql", requestEntity) ~> route ~> check(action)
  }
}
