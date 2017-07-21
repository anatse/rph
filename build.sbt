import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

name := "rph"
version := "1.0"
organization := "Mes Solutions"
scalaVersion := "2.12.2"

lazy val root = (project in file("."))
  .settings (
    scalacOptions ++= Seq("-Ypartial-unification")
  )
  .enablePlugins(PlayScala, PlayAkkaHttpServer, SbtWeb, PlayAkkaHttp2Support)
  .disablePlugins(PlayNettyServer)

//lazy val thrift = (project in file("."))
//  .settings (
//    scalacOptions ++= Seq("-Ypartial-unification"),
//    scroogeThriftSourceFolder in (Compile) := baseDirectory {
//      base => base / "src/thrift"
//    }.value,
//    scroogeThriftOutputFolder in (Compile) := baseDirectory {
//      base => base / "app/org"
//    }.value
//  ).enablePlugins(ScroogeSBT)

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.mavenLocal, "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/"
)

val silhouetteVersion = "5.+"

libraryDependencies ++= Seq (
  guice,
  ws,
  cacheApi,
  ehcache,
  filters,
  "com.typesafe.play" %%% "play-json" % "+",
  "org.apache.thrift" % "libthrift" % "0.+",
  "com.twitter" %% "scrooge-core" % "4.+" exclude("com.twitter", "libthrift"),
  "com.twitter" %% "finagle-thrift" % "6.+" exclude("com.twitter", "libthrift"),

  "com.atlassian.jwt" % "jwt-api" % "1.6.2",
  "com.atlassian.jwt" % "jwt-core" % "1.6.2",

  "com.mohiva" %% "play-silhouette" % silhouetteVersion, // exclude ("com.atlassian.jwt", "jwt-core") exclude ("com.atlassian.jwt", "jwt-core"),
  "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,

  "org.webjars" %% "webjars-play" % "2.6.0",
  "org.webjars" % "bootstrap" % "3.3.7-1" exclude("org.webjars", "jquery"),
  "org.webjars" % "jquery" % "3.2.1",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.iheart" %% "ficus" % "1.4.1",
  "com.typesafe.play" %% "play-mailer" % "6.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.0",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.1-akka-2.5.x",
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3-SNAPSHOT",

  "com.mohiva" %% "play-silhouette-testkit" % silhouetteVersion % Test,
  specs2 % Test
)

