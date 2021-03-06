package uk.vitalcode.events.api

import com.sksamuel.elastic4s.{ElasticClient, IndexType}
import uk.vitalcode.events.api.http.HttpService
import uk.vitalcode.events.api.services.{AuthService, EventService, UsersService}
import uk.vitalcode.events.api.utils.DatabaseService

import scala.concurrent.ExecutionContext

trait AppContext {
  val cxJdbcUrl: String
  val cxDbUser: String
  val cxDbPassword: String
  val cxElasticClient: ElasticClient
  val cxIndexType: IndexType
  implicit val cxExecutionContext: ExecutionContext

  lazy val databaseService = new DatabaseService(cxJdbcUrl, cxDbUser, cxDbPassword)
  lazy val usersService = new UsersService(databaseService)
  lazy val authService = new AuthService(databaseService, usersService)
  lazy val eventService = new EventService(cxElasticClient, cxIndexType)
  lazy val httpService = new HttpService(usersService, authService, eventService)
}