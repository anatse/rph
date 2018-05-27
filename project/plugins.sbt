// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.15")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")

 addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.+")

 addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.+")

 addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.+")

 addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.+")

 addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.+")

 addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.+")
