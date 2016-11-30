import sbt._

object Version {
  final val Scala = "2.12.0"
  final val ScalaTest = "3.0.1"
}

object Library {
  val scalaMeta = "org.scalameta" %% "scalameta" % "1.3.0"
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.0"
  val scalaTest = "org.scalatest" %% "scalatest" % Version.ScalaTest
}
