package ca.advtech.ar2t

import data.TweetIngest

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object TweetIngestTest {
  def main(args: Array[String]): Unit = {
    val spark = ca.advtech.ar2t.main.initSpark()
    val tweets = new TweetIngest(spark)

    // Make a RDD
    val dummyData = Array(
      entities.Product("a1123", "Hello world", List()),
      entities.Product("a1124", "Game of thrones", List()),
      entities.Product("a1125", "black island sisters ", List()),
      entities.Product("a1126", "43928493280940238", List()),
      entities.Product("a1127", "Tweets tweets tweets", List())
    )

    val distData = spark.sparkContext.parallelize(dummyData)
    val results = tweets.getTweets(spark, distData)
    results onComplete {
      case Success(value) => {
        println("Got all tweets: " + value.count())
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