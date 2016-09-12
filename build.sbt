name := "events-api"
organization := "vitalcode"
version := "0.0.1"
scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV = "2.4.7"
  val scalaTestV = "3.0.0-M15"
  val slickVersion = "3.1.1"
  val circeV = "0.4.1"
  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,

    "de.heikoseeberger" %% "akka-http-circe" % "1.6.0",

    "com.typesafe.slick" %% "slick" % slickVersion,
    "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
    "org.flywaydb" % "flyway-core" % "3.2.1",
    "com.h2database" % "h2" % "1.4.192",
    "mysql" % "mysql-connector-java" % "5.1.39",

    "com.zaxxer" % "HikariCP" % "2.4.5",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    "io.circe" %% "circe-core" % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parser" % circeV,

    "org.sangria-graphql" %% "sangria" % "0.7.3",

    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",

    "org.sangria-graphql" %% "sangria" % "0.7.0",
    "org.sangria-graphql" %% "sangria-spray-json" % "0.3.1"
  )
}

Revolver.settings
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

dockerExposedPorts := Seq(9000)
dockerEntrypoint := Seq("bin/%s" format executableScriptName.value, "-Dconfig.resource=docker.conf")