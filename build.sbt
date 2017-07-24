name := """rph"""

val scalaV = "2.12.2"
val silhouetteVersion = "5.+"
val thriftVersion = "0.9.3"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/",
  libraryDependencies ++= Seq(
    "com.atlassian.jwt" % "jwt-api" % "1.6.2",
    "com.atlassian.jwt" % "jwt-core" % "1.6.2",

    "com.vmunier" %% "scalajs-scripts" % "1.1.1",

    // Apache thrift
    "org.apache.thrift" % "libthrift" % thriftVersion,
    "com.twitter" %% "scrooge-core" % "4.18.0" exclude("com.twitter", "libthrift"),
    "com.twitter" %% "finagle-thrift" % "6.45.0" exclude("com.twitter", "libthrift"),

    "com.mohiva" %% "play-silhouette" % silhouetteVersion,
    "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
    "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,
    "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion,
    "org.webjars" %% "webjars-play" % "2.6.0",
    "org.webjars" % "bootstrap" % "3.3.7-1" exclude("org.webjars", "jquery"),
    "org.webjars" % "jquery" % "3.2.1",
    "net.codingwell" %% "scala-guice" % "4.1.0",
    "com.iheart" %% "ficus" % "1.4.1",
    "com.typesafe.play" %% "play-mailer" % "6.0.0",
    "com.typesafe.play" %% "play-mailer-guice" % "6.0.0",
    "com.enragedginger" %% "akka-quartz-scheduler" % "1.+",
    "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3-RC2",
    "com.mohiva" %% "play-silhouette-testkit" % silhouetteVersion % "test",
    specs2 % Test,
    ehcache,
    guice,
    filters
  ),
  // Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
  EclipseKeys.preTasks := Seq(compile in Compile)
).enablePlugins(PlayScala).
  aggregate(client). //clients.map(projectToRef): _*).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.6.5"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val thrift = (project in file("thrift"))
  .settings (
    scalaVersion := scalaV,
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % thriftVersion,
      "com.twitter" %% "scrooge-core" % "4.18.0" exclude("com.twitter", "libthrift"),
      "com.twitter" %% "finagle-thrift" % "6.45.0" exclude("com.twitter", "libthrift")
    ),
    scroogeThriftSourceFolder in (Compile) := baseDirectory {
      base => base / "src/thrift"
    }.value,
    scroogeThriftOutputFolder in (Compile) := baseDirectory {
      base => base / "../server/app"
    }.value,
    scroogeLanguages in (Compile) := Seq("scala", "javascript")
  ).enablePlugins(ScroogeSBT).dependsOn(sharedJvm)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

//// for Eclipse users
//EclipseKeys.skipParents in ThisBuild := false
//// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
//EclipseKeys.preTasks := Seq(compile in (server, Compile))