name := "events-api"
organization := "vitalcode"
version := "0.0.1"
scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaHttpV = "10.0.0"
  val scalaTestV = "3.0.0-M15"
  val slickVersion = "3.1.1"
  val circeV = "0.6.0"
  val macwireV = "2.2.2"
  val eventsModelV = "0.0.1"
  val jwtV = "0.9.2"
  val sprayJsonV = "1.3.2"

  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "de.heikoseeberger" %% "akka-http-circe" % "1.11.0",

    "com.typesafe.slick" %% "slick" % slickVersion,
    "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
    "org.flywaydb" % "flyway-core" % "3.2.1",

    "org.postgresql" % "postgresql" % "9.4.1212.jre7",
    "com.zaxxer" % "HikariCP" % "2.6.0",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    "io.circe" %% "circe-core" % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parser" % circeV,
    "io.spray" %% "spray-json" % sprayJsonV,

    "com.pauldijou" %% "jwt-core" % jwtV,

    "org.sangria-graphql" %% "sangria" % "0.7.3",
    "org.sangria-graphql" %% "sangria-spray-json" % "0.3.1",

    "com.sksamuel.elastic4s" %% "elastic4s-core" % "1.7.5",
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % "1.7.5" % "test",

    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test",
    "ru.yandex.qatools.embed" % "postgresql-embedded" % "1.13" % "test",

    "com.softwaremill.macwire" %% "macros" % macwireV % "provided",
    "com.softwaremill.macwire" %% "util" % macwireV,
    "com.softwaremill.macwire" %% "proxy" % macwireV,

    "vitalcode" %% "events-model" % eventsModelV
  )
}

Revolver.settings
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

parallelExecution in Test := false

dockerExposedPorts := Seq(9000)
dockerEntrypoint := Seq("bin/%s" format executableScriptName.value, "-Dconfig.resource=docker.conf")