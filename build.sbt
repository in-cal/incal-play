import com.typesafe.sbt.license.{DepModuleInfo, LicenseInfo}

organization := "org.in-cal"

name := "incal-play"

version := "0.2.3"

description := "In-Cal extension for Play Framework providing basic readonly/crud controllers, deadbolt-backed security, json formatters, etc."

isSnapshot := false

scalaVersion := "2.11.12"

resolvers ++= Seq(
  Resolver.mavenLocal
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.9",
  "be.objectify" %% "deadbolt-scala" % "2.5.1",         // Deadbolt (authentication)
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "org.in-cal" %% "incal-core" % "0.2.2",
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "bootstrap" % "3.3.7",                // Bootstrap
  "org.webjars" % "bootswatch-united" % "3.3.4+1"       // Bootstrap
)

dependencyOverrides ++= Set(
  "net.sourceforge.cssparser" % "cssparser" % "0.9.19"  // we use a new version (not 0.9.18) because of a license mismatch
)

// some of the libs' licenses are not included hence we need to provide them (override) manually
licenseOverrides := {
  case
    DepModuleInfo("net.java.dev.jna", "jna", _) // both jna and jna-platform libs have a dual LGPL / Apache 2.0 license, we choose Apache 2.0
    | DepModuleInfo("net.java.dev.jna", "jna-platform", _)
    | DepModuleInfo("org.apache.commons", _, _)
    | DepModuleInfo("commons-codec", "commons-codec", _)
    | DepModuleInfo("commons-io", "commons-io", _)
    | DepModuleInfo("commons-logging", "commons-logging", _)
    | DepModuleInfo("com.google.guava", "guava", _)
    | DepModuleInfo("com.google.inject", "guice", _)
    | DepModuleInfo("com.google.inject.extensions", _, _)
    | DepModuleInfo("cglib", "cglib-nodep", _)
    | DepModuleInfo("org.webjars", "bootswatch-united", "3.3.4+1") =>
  LicenseInfo(LicenseCategory.Apache, "Apache License v2.0", "http://www.apache.org/licenses/LICENSE-2.0")

  // javax.transaction has a dual GPL2 / CDDL license, we choose CDDL
  case DepModuleInfo("javax.transaction", "jta", "1.1") =>
    LicenseInfo(LicenseCategory.CDDL, "Common Development and Distribution License", "https://oss.oracle.com/licenses/CDDL+GPL-1.1")

  // logback libs have a dual LGPL / EPL license, we choose EPL
  case DepModuleInfo("ch.qos.logback", "logback-classic", _)
    | DepModuleInfo("ch.qos.logback", "logback-core", _) 
    | DepModuleInfo("junit", "junit", "4.12") =>
  LicenseInfo(LicenseCategory.EPL, "Eclipse Public License 1.0", "http://www.eclipse.org/legal/epl-v10.html")

  case DepModuleInfo("org.hamcrest", "hamcrest-core", "1.3") =>
    LicenseInfo(LicenseCategory.BSD, "BSD 2-clause", "https://opensource.org/licenses/BSD-2-Clause")

  case DepModuleInfo("org.slf4j", "slf4j-api", "1.7.21") =>
    LicenseInfo(LicenseCategory.MIT, "MIT License", "http://www.opensource.org/licenses/mit-license.php")
}

// POM settings for Sonatype

homepage := Some(url("https://github.com/in-cal/incal-play"))

publishMavenStyle := true

scmInfo := Some(ScmInfo(url("https://github.com/in-cal/incal-play"), "scm:git@github.com:in-cal/incal-play.git"))

developers := List(
	Developer("bnd", "Peter Banda", "peter.banda@protonmail.com", url("https://peterbanda.net")),
	Developer("valentin.groues", "Valentin Groues", "valentin.groues@uni.lu", url("https://github.com/neoflex"))
)

licenses += "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")

publishMavenStyle := true

// publishTo := sonatypePublishTo.value

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
