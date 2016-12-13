import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.GitPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderPattern
import de.heikoseeberger.sbtheader.license._
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object BuildPlugin extends AutoPlugin {

  override def requires =
    JvmPlugin && HeaderPlugin && GitPlugin

  override def trigger = allRequirements

  override def projectSettings =
    Vector(
      resolvers += Resolver.sonatypeRepo("releases"),
      // Compile settings
      scalaVersion := Version.Scala,
      crossScalaVersions := Vector(scalaVersion.value),
      scalacOptions ++= Vector(
        "-language:_",
        "-target:jvm-1.8",
        "-encoding", "UTF-8"
      ),
      unmanagedSourceDirectories.in(Compile) :=
        Vector(scalaSource.in(Compile).value),
      unmanagedSourceDirectories.in(Test) :=
        Vector(scalaSource.in(Test).value),

      // Publish settings
      organization := "com.sauldhernandez.lambdakka",
      licenses += ("BSD 3-Clause",
                   url("https://opensource.org/licenses/BSD-3-Clause")),
      mappings.in(Compile, packageBin)
        += baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",

      // Git settings
      git.useGitDescribe := true,

      // Header settings
      headers := Map("scala" -> BSD3Clause("2016", "saul"))
    )
}
