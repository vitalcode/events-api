package uk.vitalcode.events.api.test.utils

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexType
import com.sksamuel.elastic4s.testkit.ElasticSugar
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.scalatest._
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import spray.json.JsObject
import uk.vitalcode.events.api.http.routes.SchemaDefinition
import uk.vitalcode.events.api.http.{EventContext, HttpService}
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.services.{AuthService, UsersService}
import uk.vitalcode.events.api.test.utils.InMemoryPostgresStorage._
import uk.vitalcode.events.api.utils.DatabaseService

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

trait BaseTest extends WordSpec with Matchers with ScalatestRouteTest with CirceSupport with ElasticSugar {

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


  def provisionUsersList(size: Int): Seq[UserEntity] = {
    val savedUsers = (1 to size).map { _ =>
      val f = UserEntity(Some(Random.nextLong()), Random.nextString(10), Random.nextString(10), None) // TODO Permission test data
      f
    }.map(usersService.createUser)

    Await.result(Future.sequence(savedUsers), 10.seconds)
  }

  def provisionTokensForUsers(usersList: Seq[UserEntity]) = {
    val savedTokens = usersList.map(authService.createToken)
    Await.result(Future.sequence(savedTokens), 10.seconds)
  }

  def executeQuery(query: Document, vars: JsObject = JsObject.empty) = {
    Executor.execute(
      schema = SchemaDefinition.EventSchema,
      queryAst = query,
      variables = vars,
      userContext = new EventContext()
    ).await
  }
}
