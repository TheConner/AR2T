package ca.advtech.ar2t

import ca.advtech.ar2t.data.{DataIngest, DataWriter}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{Encoders, SQLContext, SparkSession}
import ca.advtech.ar2t.entities.{Review, ReviewMetadata}
import ca.advtech.ar2t.util.StringUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.{col, expr, length}

object main {
  //region Object members

  private val runtimeConfig = configuration.getConfigurationClass("execution")
  private var metadataRDD: RDD[ReviewMetadata] = null
  private var reviewRDD: RDD[Review] = null
  //endregion

  def main(args: Array[String]): Unit = {
    val spark = initSpark()
    val entity = runtimeConfig.getString("entity")


    IngestData(spark, entity)
    // At this point we will have a valid dataframe

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


    val joinedDF = movieMetaDF
      .join(uniqueMovieASIN, col("df_meta.asin") === col("df_review.asin"))
      .select(col("df_meta.asin"), col("df_meta.title"), length(col("df_meta.title")))
      .distinct()
      .cache()

    movieMetaDF.unpersist()
    movieReviewDF.unpersist()

    println("Output schema")
    joinedDF.printSchema()

    // Output
    val writer = new DataWriter(configuration.getString("data.basePath")
      + configuration.getString("data.outputPath")
      + StringUtils.genUnixTimeFileName("output", "csv"))
    writer.WriteCSV(joinedDF)
  }

  private def IngestData(spark: SparkSession, entity: String) = {
    // Check if the configurations are valid
    val ingestFromJSON = runtimeConfig.getBoolean("dataIngestFromJSON.enabled")
    val ingestFromRDD = runtimeConfig.getBoolean("dataIngestFromRDD")

    // Config sanity check
    if (ingestFromJSON && ingestFromRDD) {
      println("ERROR: both dataIngestFromJSON.enabled and dataIngestFromRDD is set. I can't ingest from both sources.")
      println("Select only one of those two options")
      throw new Exception("Invalid operation: cannot import from both JSON and RDD")
    }

    if (ingestFromJSON) {
      metadataRDD = DataIngest.ingestMetadata(spark, entity)
      reviewRDD = DataIngest.ingestData(spark, entity)

      if (runtimeConfig.getBoolean("dataIngestFromJSON.saveIngestedRDD")) {
        val writer = new DataWriter(configuration.getRDDPath(entity))
        writer.WriteDF(spark.createDataFrame(metadataRDD), "metadata")
        writer.WriteDF(spark.createDataFrame(reviewRDD), "reviews")
      }
    } else if (ingestFromRDD) {
      import spark.implicits._
      print("Reading data from RDD cache on filesystem")
      // Ingest metadataRDD and reviewRDD
      val reviewDF = DataIngest.ingestDataFrame(spark, configuration.getRDDPath(entity), "reviews")
      val metadataDF = DataIngest.ingestDataFrame(spark, configuration.getRDDPath(entity), "metadata")

      println("Schema of review data frame")
      reviewDF.printSchema()

      println("Schema of review metadata data frame")
      metadataDF.printSchema()

      // Now we have to change it to an RDD
      metadataRDD = metadataDF.as[ReviewMetadata].rdd
      reviewRDD = reviewDF.as[Review].rdd
    }
  }

  private def initSpark(): SparkSession = {
    val spark = SparkSession
      .builder()
      .appName("AR2T for X-MAP")
      .master(configuration.getString("spark.master"))
      .getOrCreate()

    configuration.configureSpark(spark.conf)
    spark.sparkContext.setLogLevel("WARN")
    return spark
  }

}
