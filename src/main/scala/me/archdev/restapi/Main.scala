package me.archdev.restapi

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexType}
import me.archdev.restapi.http.{EventContext, HttpService}
import me.archdev.restapi.services.{AuthService, UsersService}
import me.archdev.restapi.utils.{Config, DatabaseService, FlywayService}
import org.elasticsearch.common.settings.ImmutableSettings

import scala.concurrent.ExecutionContext

trait AppModule {

}

object Main extends App with Config {
  implicit val actorSystem = ActorSystem()
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val uri = ElasticsearchClientUri(elasticUrl)
  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", elasticCluster).build()
  implicit val client = ElasticClient.remote(settings, uri)
  implicit val indexType: IndexType = elasticIndex / elasticType

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)
  flywayService.migrateDatabaseSchema

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

  Http().bindAndHandle(httpService.routes, httpHost, httpPort)
}
