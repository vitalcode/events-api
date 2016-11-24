package uk.vitalcode.events.api.test.utils

import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, MediaTypes}
import akka.http.scaladsl.server
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexType
import com.sksamuel.elastic4s.testkit.ElasticSugar
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.scalatest._
import sangria.ast.Document
import sangria.renderer.QueryRenderer
import spray.json.{JsObject, JsString, _}
import uk.vitalcode.events.api.http.{EventContext, HttpService}
import uk.vitalcode.events.api.models.UserPermission._
import uk.vitalcode.events.api.models.{TokenEntity, UserEntity, UserPermission}
import uk.vitalcode.events.api.services.{AuthService, UsersService}
import uk.vitalcode.events.api.test.utils.InMemoryPostgresStorage._
import uk.vitalcode.events.api.utils.DatabaseService

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

trait BaseTest extends WordSpec with Matchers with ScalatestRouteTest with SprayJsonSupport with CirceSupport
  with GraphqlErrorSupport with ElasticSugar {

  dbProcess

  val indexName = getClass.getSimpleName.toLowerCase
  val elasticType = "type"
  implicit val indexType: IndexType = indexName / elasticType
  implicit val elasticClient = client

  implicit val databaseService2 = new DatabaseService(jdbcUrl, dbUser, dbPassword)
  val usersService = new {
    val databaseService = databaseService2
    val executionContext = executor
  } with UsersService
  val authService = new {
    val databaseService = databaseService2
    val executionContext = executor
  } with UsersService with AuthService
  val eventContext = new EventContext()

  val httpService = new HttpService(usersService, authService, eventContext)

  protected def dbTestUsers(size: Int): Seq[UserEntity] = {
    usersService.deleteAllUsers
    val savedUsers = (1 to size).map {
      case 1 => createTestUser(UserPermission.ADMIN)
      case _ => createTestUser()
    }.map(usersService.createUser)
    Await.result(Future.sequence(savedUsers), 10.seconds)
  }

  protected def createTestUser(permissions: UserPermission*): UserEntity = {
    val userPermissions = if (permissions.nonEmpty) Some(permissions.mkString(",")) else None
    UserEntity(Some(Random.nextLong()), Random.nextString(10), Random.nextString(10), userPermissions)
  }

  protected def dbTokensForTestUsers(usersList: Seq[UserEntity]) = {
    val savedTokens = usersList.map(authService.createToken)
    Await.result(Future.sequence(savedTokens), 10.seconds)
  }

  protected def graphCheck(route: server.Route, document: Document, token: Option[TokenEntity], vars: JsObject = JsObject.empty)(action: => Unit): Unit = {
    val query = QueryRenderer.render(document, QueryRenderer.Compact)
    val requestBody = JsObject(
      "query" -> JsString(query),
      "variables" -> JsString(vars.toString)
    ).compactPrint
    val requestEntity = HttpEntity(MediaTypes.`application/json`, requestBody)
    val request = Post("/graphql", requestEntity)
    token.map(addAuthorizationHeader(request, _)).getOrElse(request) ~> route ~> check(action)
  }

  protected def graphCheck(route: server.Route, document: Document, vars: JsObject)(action: => Unit): Unit = {
    graphCheck(route, document, None, vars)(action)
  }

  protected def graphCheck(route: server.Route, document: Document)(action: => Unit): Unit = {
    graphCheck(route, document, None)(action)
  }

  private def addAuthorizationHeader(request: HttpRequest, token: TokenEntity): HttpRequest = {
    request.addHeader(Authorization(HttpCredentials.createOAuth2BearerToken(token.token)))
  }

  protected def adminUser(users: Seq[UserEntity]) = {
    users.find(_.permissions.contains(UserPermission.ADMIN.toString)).get
  }

  protected def basicUser(users: Seq[UserEntity]) = {
    users.find(_.permissions.isEmpty).get
  }

  protected def userToken(user: UserEntity) = {
    Await.result(authService.login(user.username, user.password), Duration.Inf)
  }

  trait Context {
    val testUsers = dbTestUsers(2)
    val route = httpService.graphQLRoute.route
  }
}
