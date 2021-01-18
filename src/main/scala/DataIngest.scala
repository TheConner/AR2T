package ca.advtech.ar2t

import ca.advtech.ar2t.models.{JsonParseable, Review, ReviewMetadata}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.typeOf

object DataIngest {
  def ingestData(spark: SparkSession, entity: String): RDD[Review] = {
    val dataPath = configuration.getPath(entity)
    val partitions = configuration.conf.getInt("spark.parser.filePartitions")
    val metaTextFile = spark.sparkContext.textFile(dataPath, partitions)
    metaTextFile
      .mapPartitions(ParseReviewData)
  }

  def ingestMetadata(spark: SparkSession, entity: String): RDD[ReviewMetadata] = {
    val dataPath = configuration.getMetaPath(entity)
    val partitions = configuration.conf.getInt("spark.parser.filePartitions")
    val metaTextFile = spark.sparkContext.textFile(dataPath, partitions)
    metaTextFile
      .mapPartitions(ParseMetadata)
  }

  def ParseReviewData(lines: Iterator[String]): Iterator[Review] = {
    for (line <- lines) yield JsonParseable[Review].Parse(line);
  }

  def ParseMetadata(lines: Iterator[String]): Iterator[ReviewMetadata] = {
    for (line <- lines) yield JsonParseable[ReviewMetadata].Parse(line)
  }

}