package ca.advtech.ar2t
package data

import entities.{JsonParseable, Review, Product}

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * DataIngest helper
 * TODO: Currently JsonParseable provides generics support, it would be nice to use generics here too
 * we are currently not using generics as Scala gets very upset when I try to use them
 */
object DataIngest {
  def ingestDataFrame(spark: SparkSession, fname: String, name: String): DataFrame = {
    // Build the actual file name
    val actualFileName = fname.concat("_").concat(name)
    println("Attempting to read dataframe from " + actualFileName)
    spark.read.parquet(actualFileName)
  }

  def ingestData(spark: SparkSession, entity: String): RDD[Review] = {
    val dataPath = configuration.getPath(entity)
    val partitions = configuration.conf.getInt("spark.parser.filePartitions")
    val metaTextFile = spark.sparkContext.textFile(dataPath, partitions)
    metaTextFile
      .mapPartitions(ParseReviewData)
      .filter(ReviewCleaner.ReviewFilter)
  }

  def ingestMetadata(spark: SparkSession, entity: String): RDD[Product] = {
    val dataPath = configuration.getMetaPath(entity)
    val partitions = configuration.conf.getInt("spark.parser.filePartitions")
    val metaTextFile = spark.sparkContext.textFile(dataPath, partitions)
    metaTextFile
      .mapPartitions(ParseMetadata)
      .filter(MetadataCleaner.filterMetadata)
      .mapPartitions(MetadataCleaner.cleanMetadata)
  }

  //region Internal Helpers
  /**
   * Internal helper for parsing JSON to review objects. Designed to be used with spark's mapParitions method for good
   * concurrent performance.
   * @param lines Iterator of JSON strings
   * @return Iterator of Review objects
   */
  private def ParseReviewData(lines: Iterator[String]): Iterator[Review] = {
    for (line <- lines) yield JsonParseable[Review].Parse(line)
  }

  /**
   * Internal helper for parsing JSON to Product objects. Designed to be used with spark's mapParitions method
   * for good concurrent performance.
   * @param lines Iterator of JSON strings
   * @return Iterator of Product objects
   */
  private def ParseMetadata(lines: Iterator[String]): Iterator[Product] = {
    for (line <- lines) yield JsonParseable[Product].Parse(line)
  }
  //endregion
}
