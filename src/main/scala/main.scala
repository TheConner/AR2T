package ca.advtech.ar2t

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SparkSession}
import ca.advtech.ar2t.models.ReviewMetadata
import org.apache.spark.sql.functions.col

object main {
  def main(args: Array[String]): Unit = {
    var spark = SparkSession
      .builder()
      .appName("AR2T for X-MAP")
      .master(configuration.getString("spark.master"))
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    // Parse in the movie data
    val movieMetaRDD = DataIngest.ingestMetadata(spark, "movies")
    val movieReviewRDD = DataIngest.ingestData(spark, "movies")

    // Convert to dataframes
    val movieMetaDF = spark
      .createDataFrame(movieMetaRDD)
      .cache()
      .as("df_meta")

    movieMetaDF.printSchema()

    val movieReviewDF = spark
      .createDataFrame(movieReviewRDD)
      .cache()
      .as("df_review")

    movieReviewDF.printSchema()

    val joinedDF = movieMetaDF.join(movieReviewDF, col("df_meta.asin") === col("df_review.asin"))
    joinedDF.take(5).foreach(println)
  }
}
