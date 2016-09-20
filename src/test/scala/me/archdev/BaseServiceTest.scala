package me.archdev

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexType
import com.sksamuel.elastic4s.testkit.ElasticSugar
import de.heikoseeberger.akkahttpcirce.CirceSupport
import me.archdev.restapi.Main.{dbPassword => _, dbUser => _, jdbcUrl => _}
import me.archdev.restapi.http.HttpService
import me.archdev.restapi.http.routes.{ColorRepo, EventRepo, SchemaDefinition, UserRepo}
import me.archdev.restapi.models.UserEntity
import me.archdev.restapi.services.{AuthService, UsersService}
import me.archdev.restapi.utils.DatabaseService
import me.archdev.utils.InMemoryPostgresStorage._
import org.scalatest._
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import spray.json.JsObject

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

trait BaseServiceTest extends WordSpec with Matchers with ScalatestRouteTest with CirceSupport with ElasticSugar {

  dbProcess

  private val databaseService = new DatabaseService(jdbcUrl, dbUser, dbPassword)

  val indexName = getClass.getSimpleName.toLowerCase
  val elasticType = "type"
  implicit val indexType: IndexType = indexName / elasticType
  implicit val elasticClient = client

  val eventRepo = new EventRepo(new UserRepo(), new ColorRepo())
  val usersService = new UsersService(databaseService)
  val authService = new AuthService(databaseService)(usersService)
  val httpService = new HttpService(usersService, authService, eventRepo)

  def provisionUsersList(size: Int): Seq[UserEntity] = {
    val savedUsers = (1 to size).map { _ =>
      UserEntity(Some(Random.nextLong()), Random.nextString(10), Random.nextString(10))
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
      userContext = new EventRepo(new UserRepo(), new ColorRepo())
    ).await
  }
}
