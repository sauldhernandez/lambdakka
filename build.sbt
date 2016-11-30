lazy val core = project in file("core")
lazy val example = (project in file("example")).dependsOn(core)
lazy val deploy = project in file("deploy")