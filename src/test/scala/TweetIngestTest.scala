package ca.advtech.ar2t

import data.TweetIngest

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object TweetIngestTest {
  def main(args: Array[String]): Unit = {
    val spark = ca.advtech.ar2t.main.initSpark()
    val tweets = new TweetIngest(spark)

    val rateLimits = Await.result(tweets.getRestClient().rateLimits(), 5.seconds)
    print(rateLimits.data.resources.search)

    spark.close()
  }
}