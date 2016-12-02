package uk.vitalcode.events.api

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexType}
import org.elasticsearch.common.settings.ImmutableSettings
import uk.vitalcode.events.api.utils.{Config, FlywayService}

object Main extends App with Config {
  implicit val actorSystem = ActorSystem()
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val uri = ElasticsearchClientUri(elasticUrl)
  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", elasticCluster).build()
  val client = ElasticClient.remote(settings, uri)
  val indexType: IndexType = elasticIndex / elasticType

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)
  flywayService.migrateDatabaseSchema

  val appContext = new AppContext {
    val cxJdbcUrl = jdbcUrl
    val cxDbUser = dbUser
    val cxDbPassword = dbPassword
    val cxElasticClient = client
    val cxIndexType = indexType
    val cxExecutionContext = actorSystem.dispatcher
  }

  Http().bindAndHandle(appContext.httpService.routes, httpHost, httpPort)
}
