package ca.advtech.ar2t
package data

import ca.advtech.ar2t.entities.{Product, SimplifiedTweet}

import scala.concurrent.ExecutionContext.Implicits.global
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RateLimit, RatedData, StatusSearch, Tweet}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class TweetIngest extends Serializable {
  private val maxConcurrentRequests = 2
  private val restClient = TwitterRestClient()
  private var tweetCache = scala.collection.mutable.Map[String, List[SimplifiedTweet]]()
  private var repsonses = ListBuffer[Product]()
  private var currentRateLimit: RateLimit = null
  private var isPaused = false
  private var requestCount = 0
  private var currentRequests = 0

  private def searchTweet(query: String): Future[RatedData[StatusSearch]] = {
    if (currentRateLimit != null) {
      if (currentRateLimit.remaining == 0) {
        println("Hit rate limit, pausing for " + currentRateLimit.reset.getEpochSecond.toString)
        // Get the pause time
        Thread.sleep(currentRateLimit.reset.toEpochMilli)
      }
    }

    println("Making REST call for query " + query)
    currentRequests += 1
    restClient.searchTweet(query)
  }

  def getTweets(spark: SparkSession, requests: RDD[Product]): Future[Dataset[Product]] = Future {
    import spark.implicits._
    val collectedReq = requests.collect()
    val blah = collectedReq.map(product => {
      while (currentRequests >= maxConcurrentRequests) {
        println("Concurrent request limit hit, pausing")
        Thread.sleep(1000)
      }

      val query = product.title.toLowerCase()
      if (tweetCache.keys.exists(_ == query)) {
        Product(product.asin, product.title, tweetCache(query))
      } else {
        val search = searchTweet(query)
        search onComplete {
          case Success(result) => {
            requestCount += 1
            currentRequests -= 1
            currentRateLimit = result.rate_limit

            // Add the data to the response
            tweetCache += (query -> result.data.statuses.map(t => SimplifiedTweet(t.created_at, t.favorite_count, t.id, t.lang, t.retweet_count, t.source, t.text, t.truncated)))

            repsonses += Product(product.asin, product.title,
              result.data.statuses.map(t => SimplifiedTweet(t.created_at, t.favorite_count, t.id, t.lang, t.retweet_count, t.source, t.text, t.truncated)))
          }
          case Failure(err) => {
            println("Error in getTweets")
            requestCount += 1
            currentRequests -= 1
            println(err.getMessage)
            println(err.getStackTrace.mkString("\n"))
          }
        }
      }
    })

    // Wait to ensure we get all the responses
    while (requestCount + 1 < collectedReq.length) {
      println("Waiting for responses, got " + requestCount + " of " + collectedReq.length)
      Thread.sleep(5000)
    }

    // With all of that done, we can build a RDD from this
    spark.createDataFrame(repsonses).as[Product]
  }

}
