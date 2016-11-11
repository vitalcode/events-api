package uk.vitalcode.events.api.test.graphQL

import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
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

    "retrieve user information (me) for authorize user" in new Context {
      val testUser = testUsers(1)
      me(testUser, route) {
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
  }


  private def responseToken(token: String) = JsObject("data" -> JsObject("token" -> JsString(token)))

  private def me(user: UserEntity, route: server.Route)(action: => Unit) = {
    val query =
      graphql"""
        {
          me {
            id,
            username
          }
        }
        """
    val token = Await.result(authService.signIn(user.username, user.password), Duration.Inf)
    val requestEntity = HttpEntity(MediaTypes.`application/json`,
      graphRequest(query)
    )
    Post("/graphql", requestEntity).addHeader(Authorization(HttpCredentials.createOAuth2BearerToken(token.get.token))) ~> route ~> check(action)
  }

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
