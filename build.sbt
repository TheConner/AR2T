name := "AR2T"

version := "0.1"

scalaVersion := "2.12.12"

// Apache spark
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.1.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.1.0"

// For config files
libraryDependencies += "com.typesafe" % "config" % "1.4.1"

// For JSON
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.4"

// For CSV
libraryDependencies += "com.univocity" % "univocity-parsers" % "2.9.1"

idePackagePrefix := Some("ca.advtech.ar2t")

