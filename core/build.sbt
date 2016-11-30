enablePlugins(HeaderPlugin, BuildPlugin)

name := "lambakka-core"

libraryDependencies ++= Vector(
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "io.spray" %%  "spray-json" % "1.3.2",
//  Library.scalaMeta,
  Library.akkaHttp,
  Library.scalaTest % "test"
)
