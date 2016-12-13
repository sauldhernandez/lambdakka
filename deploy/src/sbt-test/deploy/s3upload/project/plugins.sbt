{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("com.sauldhernandez.lambdakka" % "lambdakka-deploy" % pluginVersion)
}

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5"
)