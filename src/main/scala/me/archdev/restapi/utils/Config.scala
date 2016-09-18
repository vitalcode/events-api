package me.archdev.restapi.utils

import com.typesafe.config.ConfigFactory

trait Config {
  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")
  private val databaseConfig = config.getConfig("database")
  private val elasticConfig = config.getConfig("elastic")

  val httpHost = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")

  val jdbcUrl = databaseConfig.getString("url")
  val dbUser = databaseConfig.getString("user")
  val dbPassword = databaseConfig.getString("password")

  val elasticUrl = elasticConfig.getString("url")
  val elasticIndex = elasticConfig.getString("index")
  val elasticType = elasticConfig.getString("type")
  val elasticCluster = elasticConfig.getString("cluster")
}
