package uk.vitalcode.events.api.test.utils

import ru.yandex.qatools.embed.postgresql.PostgresStarter
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.{Credentials, Net, Storage, Timeout}
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig
import ru.yandex.qatools.embed.postgresql.distribution.Version
import uk.vitalcode.events.api.utils.FlywayService


object InMemoryPostgresStorage extends Config {
  val jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName"

  def runFlywayService() = {
    val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)
    flywayService.dropDatabase.migrateDatabaseSchema
  }

  lazy val dbProcess = if (dbEmbedded) {
    val psqlConfig = new PostgresConfig(
      Version.V9_5_0,
      new Net(dbHost, dbPort),
      new Storage(dbName), new Timeout(),
      new Credentials(dbUser, dbPassword)
    )
    val psqlInstance = PostgresStarter.getDefaultInstance
    psqlInstance.prepare(psqlConfig).start()

    runFlywayService()
  }
  else {
    runFlywayService()
  }
}