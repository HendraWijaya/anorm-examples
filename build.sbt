organization := "localhost"

name := "anorm-examples"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

fork in Test := true

parallelExecution in Test := false

resolvers ++= Seq(
  Resolver.url("Play Ivy Repo", new java.net.URL("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns)
)

libraryDependencies ++=
  "play" %% "anorm" % "2.0-beta" ::
  "org.scalatest" %% "scalatest" % "1.6.1"::
  "com.h2database" % "h2" % "1.3.162" ::
  Nil
