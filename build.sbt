organization := "org.incal"

name := "incal-play"

version := "0.0.17"

scalaVersion := "2.11.12"

resolvers ++= Seq(
  Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.6",
  "org.webjars" %% "webjars-play" % "2.5.0",
  "be.objectify" %% "deadbolt-scala" % "2.5.1",
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "org.incal" %% "incal-core" % "0.0.4"
)
