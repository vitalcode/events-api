package uk.vitalcode.events.api.test.utils

import de.flapdoodle.embed.process.runtime.Network._
import ru.yandex.qatools.embed.postgresql.PostgresStarter
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.{Credentials, Net, Storage, Timeout}
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig
import ru.yandex.qatools.embed.postgresql.distribution.Version
import uk.vitalcode.events.api.utils.FlywayService

object InMemoryPostgresStorage {
  val dbHost = getLocalHost.getHostAddress
  val dbPort = 25535
  val dbName = "event_test"
  val dbUser = "user"
  val dbPassword = "password"
  val jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName"

  lazy val dbProcess = {
    val psqlConfig = new PostgresConfig(
      Version.V9_5_0, new Net(dbHost, dbPort),
      new Storage(dbName), new Timeout(),
      new Credentials(dbUser, dbPassword)
    )
    val psqlInstance = PostgresStarter.getDefaultInstance
    val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

    val process = psqlInstance.prepare(psqlConfig).start()
    flywayService.dropDatabase.migrateDatabaseSchema
    process
  }
}
