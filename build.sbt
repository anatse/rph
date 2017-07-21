
name := "smes"
version := "1.0"
organization := "Mes Solutions"
scalaVersion := "2.12.2"

lazy val root = (project in file("."))
  .settings (
    scalacOptions ++= Seq("-Ypartial-unification")
  )
  .enablePlugins(PlayScala, PlayAkkaHttp2Support, PlayAkkaHttpServer, SbtWeb)
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

resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/"
resolvers ++= Seq(Resolver.mavenLocal, "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= Seq (
  guice,
  ws,
  cacheApi,
  ehcache,
  "com.typesafe.play" %%% "play-json" % "+",
  "org.apache.thrift" % "libthrift" % "0.+",
  "com.twitter" %% "scrooge-core" % "4.+" exclude("com.twitter", "libthrift"),
  "com.twitter" %% "finagle-thrift" % "6.+" exclude("com.twitter", "libthrift"),
  "org.pac4j" % "play-pac4j" % "4.+",
  "org.pac4j" % "pac4j-oauth" % "2.0.0",
  "org.pac4j" % "pac4j-oidc" % "2.0.0",
  "org.pac4j" % "pac4j-openid" % "2.0.0",
  "org.pac4j" % "pac4j-jwt" % "2.0.0",
  "org.pac4j" % "pac4j-http" % "2.0.0",
  "org.pac4j" % "pac4j-sql" % "2.0.0",

  specs2 % Test
)

