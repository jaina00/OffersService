
name := "OffersService"

version := "0.1"

scalaVersion := "2.12.10"

val akkaV = "2.5.25"
val akkaHttpV = "10.1.5"
val scalaTestV = "3.0.3"
val mockitoScalaVersion = "1.5.10"
val circeV = "0.12.3"

val ProjectDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV  % "it,test",
  "org.scalatest" %% "scalatest" % scalaTestV % "it,test",
  "org.mockito" %% "mockito-scala" % mockitoScalaVersion % "it,test",

  // logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.2.1",

  // redis
  "com.github.etaty" %% "rediscala" % "1.8.0",
  "com.github.kstyrc" % "embedded-redis" % "0.6" % "test",

  // json
  "de.heikoseeberger" %% "akka-http-circe" % "1.17.0",
  "io.circe" %% "circe-core" % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  "io.circe" %% "circe-jawn" % circeV,

  //cats
  "org.typelevel" %% "cats-core" % "2.0.0"
)

scalacOptions += "-Ypartial-unification"

lazy val IntegrationTest = config("it") extend (Test)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    libraryDependencies ++= ProjectDependencies
  )