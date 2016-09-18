package me.archdev.restapi

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexType}
import me.archdev.restapi.http.HttpService
import me.archdev.restapi.http.routes.EventRepo
import me.archdev.restapi.services.{AuthService, UsersService}
import me.archdev.restapi.utils.{Config, DatabaseService, FlywayService}
import org.elasticsearch.common.settings.ImmutableSettings

import scala.concurrent.ExecutionContext

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

  val databaseService = new DatabaseService(jdbcUrl, dbUser, dbPassword)
  val usersService = new UsersService(databaseService)
  val authService = new AuthService(databaseService)(usersService)
  val eventRepo = new EventRepo()

  val httpService = new HttpService(usersService, authService, eventRepo)

  Http().bindAndHandle(httpService.routes, httpHost, httpPort)
}
