// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

//addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.18.0")

//addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.7.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.+")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.+")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.+")

//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")