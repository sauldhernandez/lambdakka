name := "lambdakka-deploy"

scalaVersion := "2.10.6"

sbtPlugin := true

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-cloudformation" % "1.11.61",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.61"
)