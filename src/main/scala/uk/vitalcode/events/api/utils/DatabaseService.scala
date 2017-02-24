package uk.vitalcode.events.api.utils

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

class DatabaseService(jdbcUrl: String, dbUser: String, dbPassword: String) {

  // TODO investigate hikari usage in docker swarm environment
//  private val hikariConfig = new HikariConfig()
//  hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
//  hikariConfig.setJdbcUrl(jdbcUrl)
//  hikariConfig.setUsername(dbUser)
//  hikariConfig.setPassword(dbPassword)
//
//
//  private val dataSource = new HikariDataSource(hikariConfig)

  val driver = slick.driver.PostgresDriver

  import driver.api._

//  val db = Database.forDataSource(dataSource)

  val db = Database.forURL(
    url = jdbcUrl,
    user = dbUser,
    password = dbPassword,
    driver = "org.postgresql.Driver"
  )
  db.createSession()
}
