name := "async-transactions"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.11.1", "2.10.4")

organization := "be.objectify"

libraryDependencies ++= Seq(
  javaJpa % "provided"
)

