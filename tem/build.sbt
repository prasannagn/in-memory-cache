name := "in-memory-cache"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "com.github.blemale" %% "scaffeine" % "2.5.0"
libraryDependencies += "io.dropwizard.metrics"          % "metrics-core" % "3.2.6"