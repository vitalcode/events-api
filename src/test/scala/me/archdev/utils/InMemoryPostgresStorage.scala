package me.archdev.utils

import me.archdev.restapi.utils.FlywayService

object InMemoryPostgresStorage {
//  val dbHost = getLocalHost.getHostAddress
//  val dbPort = 25535
//  val dbName = "database-name"
//  val dbUser = "user"
//  val dbPassword = "password"
  //val jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName"


  val dbHost = "localhost"
  val dbPort = 3306
  val dbName = "test"
  val dbUser = "root"
  val dbPassword = "root"
  val jdbcUrl = s"jdbc:mysql://$dbHost:$dbPort/$dbName?characterEncoding=UTF-8"

  lazy val dbProcess = {
//    val psqlConfig = new PostgresConfig(
//      Version.V9_5_0, new Net(dbHost, dbPort),
//      new Storage(dbName), new Timeout(),
//      new Credentials(dbUser, dbPassword)
//    )
//    val psqlInstance = PostgresStarter.getDefaultInstance

    val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

    //val process = psqlInstance.prepare(psqlConfig).start()
    flywayService.dropDatabase.migrateDatabaseSchema
    //process
  }
}
