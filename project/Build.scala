import sbt._

object Build extends Build {

  lazy val root = Project("root", file(".")) dependsOn eventsModel
  lazy val eventsModel =
    RootProject(uri("https://github.com/vitalcode/events-model.git"))
}