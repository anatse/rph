name := """rph"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.12.1"

libraryDependencies ++= Seq (
  jdbc,
  filters,
  cache,
  ws,
  "com.orientechnologies" % "orientdb-graphdb" % "2.+",
  "org.webjars" % "webjars-play_2.11" % "2.6.0-M1",
  "org.webjars" % "react" % "15.3.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-M1" % Test
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator