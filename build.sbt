lazy val lambdakka =
  project.in(file(".")).enablePlugins(AutomateHeaderPlugin, GitVersioning)

libraryDependencies ++= Vector(
  Library.scalaTest % "test"
)

initialCommands := """|import default.lambdakka._
                      |""".stripMargin
