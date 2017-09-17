import sbt.Keys.resolvers

name := """rph"""

val scalaV = "2.12.3"
val silhouetteVersion = "5.+"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  scalacOptions := Seq("-unchecked", "-deprecation", "-opt-inline-from"),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/",
  libraryDependencies ++= Seq(
//    "com.atlassian.jwt" % "jwt-api" % "1.6.2",
//    "com.atlassian.jwt" % "jwt-core" % "1.6.2",
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
    "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3-RC2",

    "net.codingwell" %% "scala-guice" % "4.1.0",
    "com.iheart" %% "ficus" % "1.4.1",
    "com.typesafe.play" %% "play-mailer" % "6.0.0",
    "com.typesafe.play" %% "play-mailer-guice" % "6.0.0",
    "com.enragedginger" %% "akka-quartz-scheduler" % "1.+",

    // Database slick & postgresql
    "com.typesafe.play" %% "play-slick" % "3.0.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.3.0",
    "com.github.tototoshi" %% "scala-csv" % "1.3.4",
    "org.postgresql" % "postgresql" % "42.1.3",

    // mongoDB
    "org.reactivemongo" %% "play2-reactivemongo" % "0.12.+",

    // json
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.+",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.+",

    "com.mohiva" %% "play-silhouette-testkit" % silhouetteVersion % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test",
    "com.h2database" % "h2" % "1.4.196" % "test",
    specs2 % Test,

    // Play libraries
    ehcache,
    guice,
    filters
  )
).enablePlugins(PlayScala).
  aggregate(client).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  coverageEnabled := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.6.5",
    "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
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
