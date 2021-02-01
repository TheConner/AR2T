import sbt.Keys.libraryDependencies

name := "AR2T"

version := "0.1"

scalaVersion := "2.12.12"

resolvers += Resolver.sonatypeRepo("releases")

val sparkDeps = Seq(
  "org.apache.spark" %% "spark-core" % "3.1.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.1.0" % "provided"

)

val commonDeps = Seq(
  // For config files
  "com.typesafe" % "config" % "1.4.1",
  // For JSON
  "com.typesafe.play" %% "play-json" % "2.7.4",
  // For CSV
  "com.univocity" % "univocity-parsers" % "2.9.1",
  // For twitter
  "com.danielasfregola" %% "twitter4s" % "7.0"
)

lazy val app = (project in file("."))
  .settings(
    libraryDependencies ++= commonDeps,
    libraryDependencies ++= sparkDeps,
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