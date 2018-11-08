organization := "org.in-cal"

name := "incal-play"

version := "0.0.21"

description := "In-Cal extension for Play Framework providing basic readonly/crud controllers, deadbolt-backed security, json formatter, etc."

isSnapshot := false

scalaVersion := "2.11.12"

resolvers ++= Seq(
  Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.6",
  "org.webjars" %% "webjars-play" % "2.5.0",
  "be.objectify" %% "deadbolt-scala" % "2.5.1",
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "org.in-cal" %% "incal-core" % "0.0.6"
)


// POM settings for Sonatype

homepage := Some(url("https://in-cal.org"))

publishMavenStyle := true

scmInfo := Some(ScmInfo(url("https://github.com/in-cal/incal-play"), "scm:git@github.com:in-cal/incal-play.git"))

developers := List(Developer("bnd", "Peter Banda", "peter.banda@protonmail.com", url("https://peterbanda.net")))

licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")

publishMavenStyle := true

// publishTo := sonatypePublishTo.value

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
