package ca.advtech.ar2t

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SparkSession}
import argonaut._
import Argonaut._

import ca.advtech.ar2t.models.ReviewMetadata

object main {
  def main(args: Array[String]): Unit = {
    var spark = SparkSession
      .builder()
      .appName("AR2T for X-MAP")
      .master("local")
      .getOrCreate()

    importJsonData(spark, "movies")
  }

  private def importJsonData(spark: SparkSession, entity: String): Unit = {
    // Paths of the main dataset, and the metadata
    val dataPath = configuration.getPath(entity)
    val metaPath = configuration.getMetaPath(entity)

    // Load metadata in parallel
    // We lose lots of the DF related fun but we get a performance boost on multicore systems
    val metaTextFile = spark.sparkContext.textFile(metaPath, 8)
    val parsedMetaTextFile = metaTextFile.mapPartitions(x => parseLine(x, spark))
    parsedMetaTextFile.take(5).foreach(println)
  }

  private def parseLine(iterators: Iterator[String], spark: SparkSession) = {
    for (line <- iterators) yield line.decodeOption[ReviewMetadata].getOrElse(Nil)
  }
}
