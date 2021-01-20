package ca.advtech.ar2t

import ca.advtech.ar2t.Data.{DataIngest, DataWriter}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SparkSession}
import ca.advtech.ar2t.models.ReviewMetadata
import ca.advtech.ar2t.util.StringUtils
import org.apache.spark.sql.functions.col

object main {
  def main(args: Array[String]): Unit = {
    var spark = SparkSession
      .builder()
      .appName("AR2T for X-MAP")
      .master(configuration.getString("spark.master"))
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    // Parse in the movie data
    val movieMetaRDD = DataIngest.ingestMetadata(spark, "movies")
    val movieReviewRDD = DataIngest.ingestData(spark, "movies")

    // Convert to dataframes
    val movieMetaDF = spark
      .createDataFrame(movieMetaRDD)
      .cache()
      .as("df_meta")

    printf("Metadata DataFrame")
    movieMetaDF.printSchema()

    val movieReviewDF = spark
      .createDataFrame(movieReviewRDD)
      .cache()
      .as("df_review")

    printf("Review DataFrame")
    movieReviewDF.printSchema()

    // Get all unique ASINs that we have reviews for
    val uniqueMovieASIN = movieReviewDF.select("asin").distinct()


    val joinedDF = movieMetaDF
      .join(uniqueMovieASIN, col("df_meta.asin") === col("df_review.asin"))
      .select(col("df_meta.asin"), col("df_meta.title"))
      .distinct()
      .cache()

    // Output
    val writer = new DataWriter(configuration.getString("data.basePath")
      + configuration.getString("data.outputPath")
      + StringUtils.genUnixTimeFileName("output", "csv"))
    writer.WriteCSV(joinedDF)


  }
}
