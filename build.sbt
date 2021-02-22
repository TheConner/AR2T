import sbt.Keys.libraryDependencies

name := "AR2T"

version := "0.1"

scalaVersion := "2.12.13"

val Spark = "3.0.1"
val Typesafe = "1.4.1"
val Akka = "2.6.12"
val AkkaHttp = "10.2.3"
val AkkaHttpJson4s = "1.35.3"
val Json4s = "3.6.6"
val Specs2 = "4.10.6"
val ScalaLogging = "3.9.2"
val RandomDataGenerator = "2.8"

val commonDeps = Seq(
  "org.apache.spark" %% "spark-core" % Spark,
  "org.apache.spark" %% "spark-sql" % Spark,
  // For config files
  "com.typesafe" % "config" % "1.4.1",
  // For JSON
  "com.typesafe.play" %% "play-json" % "2.9.2",
  // For CSV
  "com.univocity" % "univocity-parsers" % "2.9.1",
  "com.danielasfregola" %% "twitter4s" % "7.2-SNAPSHOT" from("file:///home/conner/IdeaProjects/twitter4s/target/scala-2.12/twitter4s_2.12-7.2-SNAPSHOT.jar"),
  "com.typesafe.akka" %% "akka-actor" % Akka,
  "com.typesafe.akka" %% "akka-stream" % Akka,
  "com.typesafe.akka" %% "akka-http" % AkkaHttp,
  "de.heikoseeberger" %% "akka-http-json4s" % AkkaHttpJson4s,
  "org.json4s" %% "json4s-native" % Json4s,
  "org.json4s" %% "json4s-ext" % Json4s,
  "com.typesafe.scala-logging" %% "scala-logging" % ScalaLogging,
  "org.specs2" %% "specs2-core" % Specs2 % "test",
  "org.specs2" %% "specs2-mock" % Specs2 % "test",
  "com.typesafe.akka" %% "akka-testkit" % Akka % "test",
  "com.danielasfregola" %% "random-data-generator" % RandomDataGenerator % "test"
)
/**
 *
 * // For twitter
  "com.danielasfregola" %% "twitter4s" % "7.2-SNAPSHOT" from("file:///home/conner/IdeaProjects/twitter4s/target/scala-2.12/twitter4s_2.12-7.2-SNAPSHOT.jar"),
  "com.typesafe" % "config" % Typesafe,
  "com.typesafe.akka" %% "akka-actor" % Akka,
  "com.typesafe.akka" %% "akka-stream" % Akka,
  "com.typesafe.akka" %% "akka-http" % AkkaHttp,
  "de.heikoseeberger" %% "akka-http-json4s" % AkkaHttpJson4s,
  "org.json4s" %% "json4s-native" % Json4s,
  "org.json4s" %% "json4s-ext" % Json4s,
  "com.typesafe.scala-logging" %% "scala-logging" % ScalaLogging,
  "com.typesafe.akka" %% "akka-testkit" % Akka % "test",
  "com.danielasfregola" %% "random-data-generator" % RandomDataGenerator % "test"
 */

lazy val app = (project in file("."))
  .settings(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= commonDeps,
    run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated,
    runMain in Compile := Defaults.runMainTask(fullClasspath in Compile, runner in(Compile, run)).evaluated,
    assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      cp filter { f => {
        println(f.data.getName)
        f.data.getName.contains("spark") || f.data.getName.startsWith("spark")
      }
      }
    },
    idePackagePrefix := Some("ca.advtech.ar2t")
  )

idePackagePrefix := Some("ca.advtech.ar2t")