import sbt.Keys.resolvers

name := """rph"""

val scalaV = "2.12.3"
val silhouetteVersion = "5.+"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(client, clientAdmin),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  LessKeys.compress in Assets := true,
  includeFilter in (Assets, LessKeys.less) := "*.less",
  scalacOptions := Seq("-unchecked", "-deprecation", "-opt-inline-from"),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  resolvers ++= Seq (
    "Atlassian Releases" at "https://maven.atlassian.com/public/",
    "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
  ),
  libraryDependencies ++= Seq(
    // Scala js scripts
    "com.vmunier" %% "scalajs-scripts" % "1.1.1",

    "com.mohiva" %% "play-silhouette" % silhouetteVersion exclude ("com.atlassian.jwt", "jwt-api") exclude ("com.atlassian.jwt", "jwt-core"),
    "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
    "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,
    "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion,
    "org.webjars" %% "webjars-play" % "2.6.0",

    // Javascript libraries
    "org.webjars" % "bootstrap" % "3.3.7-1" exclude("org.webjars", "jquery"),
    "org.webjars" % "jquery" % "3.2.1",
    "org.webjars" % "fuelux" % "3.3.1",
    "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3-RC2",

    "net.codingwell" %% "scala-guice" % "4.+",
    "com.iheart" %% "ficus" % "1.4.1",
    "com.typesafe.play" %% "play-mailer" % "6.+",
    "com.typesafe.play" %% "play-mailer-guice" % "6.+",
    "com.enragedginger" %% "akka-quartz-scheduler" % "1.+",

    // mongoDB
    "org.reactivemongo" %% "play2-reactivemongo" % "0.12.6-play26",

    // json
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.+",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.+",

    "com.mohiva" %% "play-silhouette-testkit" % silhouetteVersion % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test",
    specs2 % Test,

    // Play libraries
    ehcache,
    guice,
    filters
  )
).enablePlugins(PlayScala, SbtWeb).
  aggregate(client, clientAdmin).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  coverageEnabled := false,
  mainClass in Compile := Some("phr.ProductJS"),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.+",
    "com.lihaoyi" %%% "scalatags" % "0.6.+",
    "be.doeraene" %%% "scalajs-jquery" % "0.9.+"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val clientAdmin = (project in file("clientAdmin")).settings(
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  coverageEnabled := false,
  mainClass in Compile := Some("phr.AdminJS"),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.+",
    "com.lihaoyi" %%% "scalatags" % "0.6.+",
    "be.doeraene" %%% "scalajs-jquery" % "0.9.+"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
