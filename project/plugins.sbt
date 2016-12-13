addSbtPlugin("com.typesafe.sbt"  % "sbt-git"        % "0.8.5")
addSbtPlugin("de.heikoseeberger" % "sbt-header"     % "1.6.0")


libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}