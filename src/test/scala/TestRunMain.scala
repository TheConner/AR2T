package ca.advtech.ar2t

import data.{DataWriter, TweetIngest}
import entities.{Product, Review}
import main.{IngestData, initSpark, metadataRDD, reviewRDD, runtimeConfig}
import util.StringUtils

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.col

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object TestRunMain {
  private val runtimeConfig = configuration.getConfigurationClass("execution")
  private var metadataRDD: RDD[Product] = null
  private var reviewRDD: RDD[Review] = null

  def main(args: Array[String]): Unit = {
    val spark = initSpark()
    val entity = runtimeConfig.getString("entity")

    IngestData(spark, entity)

    metadataRDD = ca.advtech.ar2t.main.metadataRDD
    reviewRDD = ca.advtech.ar2t.main.reviewRDD

    // Convert to dataframes
    val movieMetaDF = spark
      .createDataFrame(metadataRDD)
      .as("df_meta")
      .cache()

    printf("Metadata DataFrame")
    movieMetaDF.printSchema()

    val movieReviewDF = spark
      .createDataFrame(reviewRDD)
      .as("df_review")
      .cache()

    printf("Review DataFrame")
    movieReviewDF.printSchema()

    // Get all unique ASINs that we have reviews for
    val uniqueMovieASIN = movieReviewDF.select("asin").distinct()

    val joinedDF: Array[Product] = movieMetaDF
      .join(uniqueMovieASIN, col("df_meta.asin") === col("df_review.asin"))
      .select(col("df_meta.asin"), col("df_meta.title"))
      .distinct()
      .take(10)
      .map(p => Product(p.getString(0), p.getString(1)))

    val tweetIngest = new TweetIngest(spark)
    val tweets = tweetIngest.getTweets(spark, spark.sparkContext.parallelize[Product](joinedDF))
    tweets onComplete {
      case Success(value) => {
        println("Got all tweets: " + value.count())
        tweetIngest.writeCache()

        // Write tweets
        val dfWriter = new DataWriter(configuration.getRDDPath(entity))
        dfWriter.WriteDS(value, "metadata_tweets")

        val jsonWriter = new DataWriter(configuration.getString("data.basePath")
          + configuration.getString("data.outputPath")
          + StringUtils.genUnixTimeFileName("output", "json"))

        jsonWriter.WriteJSON(value)
        spark.close()
      }
      case Failure(exception) => {
        println("--- ERROR ---")
        println(exception.getMessage)
        println(exception.getStackTrace.mkString("\n"))
        spark.close()
      }
    }
  }
}
