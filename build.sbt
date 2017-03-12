name := """rph"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq (
  jdbc,
  filters,
  cache,
  ws,
  "io.surfkit" %% "reactive-gremlin" % "0.0.1",
  "com.orientechnologies" % "orientdb-graphdb" % "2.+",
  "com.feth" %% "play-authenticate" % "0.8.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
