package uk.vitalcode.events.api.test.utils

import com.typesafe.config.ConfigFactory

trait Config {
  private val config = ConfigFactory.load()
  private val databaseConfig = config.getConfig("database")

  lazy val dbHost = databaseConfig.getString("host")
  lazy val dbPort = if (dbEmbedded) 25535 else databaseConfig.getInt("port")
  lazy val dbName = databaseConfig.getString("name")
  lazy val dbUser = databaseConfig.getString("user")
  lazy val dbPassword = databaseConfig.getString("password")
  lazy val dbEmbedded = databaseConfig.getBoolean("embedded")
}