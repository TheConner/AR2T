package ca.advtech.ar2t
package Data

import models.{JsonParseable, Review, ReviewMetadata}

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
 * DataIngest helper
 * TODO: Currently JsonParseable provides generics support, it would be nice to use generics here too
 * we are currently not using generics as Scala gets very upset when I try to use them
 */
object DataIngest {
  def ingestData(spark: SparkSession, entity: String): RDD[Review] = {
    val dataPath = configuration.getPath(entity)
    val partitions = configuration.conf.getInt("spark.parser.filePartitions")
    val metaTextFile = spark.sparkContext.textFile(dataPath, partitions)
    metaTextFile
      .mapPartitions(ParseReviewData)
      .filter(ReviewCleaner.ReviewFilter)
  }

  def ingestMetadata(spark: SparkSession, entity: String): RDD[ReviewMetadata] = {
    val dataPath = configuration.getMetaPath(entity)
    val partitions = configuration.conf.getInt("spark.parser.filePartitions")
    val metaTextFile = spark.sparkContext.textFile(dataPath, partitions)
    metaTextFile
      .mapPartitions(ParseMetadata)
      .filter(MetadataCleaner.filterMetadata)
      .mapPartitions(MetadataCleaner.cleanseMetadata)
  }

  def ParseReviewData(lines: Iterator[String]): Iterator[Review] = {
    for (line <- lines) yield JsonParseable[Review].Parse(line);
  }

  def ParseMetadata(lines: Iterator[String]): Iterator[ReviewMetadata] = {
    for (line <- lines) yield JsonParseable[ReviewMetadata].Parse(line)
  }
}
