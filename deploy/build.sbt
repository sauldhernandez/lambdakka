name := "lambdakka-deploy"

scalaVersion := "2.10.6"

sbtPlugin := true

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-cloudformation" % "1.11.61",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.61",
  "commons-codec" % "commons-codec" % "1.10",
  "net.jcazevedo" %% "moultingyaml" % "0.4.0"
)

scalacOptions := Vector(
  "-language:_",
  "-encoding", "UTF-8"
)

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false