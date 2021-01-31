package ca.advtech.ar2t
package data

import ca.advtech.ar2t.entities.{Product, SimplifiedTweet, TweetSearchResults}

import scala.concurrent.ExecutionContext.Implicits.global
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.enums.Resource
import com.danielasfregola.twitter4s.entities.{RateLimit, RatedData, StatusSearch, Tweet}
import com.danielasfregola.twitter4s.exceptions.TwitterException
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Dataset, Encoders, SaveMode, SparkSession}

import java.nio.file.{Files, Path, Paths}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future, duration}
import scala.util.control.Exception
import scala.util.{Failure, Success}

class TweetIngest(spark: SparkSession) extends Serializable {
  private val config = configuration.getConfigurationClass("data.tweets")
  private val maxConcurrentRequests = config.getInt("maxConcurrentRequests")
  private val restClient = TwitterRestClient()
  private var tweetCache = scala.collection.mutable.Map[String, List[SimplifiedTweet]]()
  //private var tweetCache: Dataset[TweetSearchResults] = null
  private var repsonses = ListBuffer[Product]()
  private var currentRateLimit: RateLimit = getRateLimit()
  private var requestCount = 0
  private var currentRequests = 0
  private var totalRequests = 0
  // Min number of time to pause before requests, in millis
  private val reqDelay = config.getInt("requestDelay")
  private val cacheFile = configuration.getString("data.basePath") + config.getString("writeTweetPath") + config.getString("writeTweetFile")

  // Need to load the cache on startup
  loadCache()

  // On create we will want to seed our tweet cache
  private def loadCache(): Unit = {
    import spark.implicits._

    println("TweetIngest: Loading cache")

    if (Files.exists(Paths.get(cacheFile))) {
      println("Cache file exists")
      val schema = Encoders.product[TweetSearchResults].schema
      val tweetCacheDS = spark.read.load(cacheFile).as[TweetSearchResults].collect()
      tweetCacheDS.foreach(m => tweetCache += (m.query -> m.tweets))

      //tweetCacheDS.map(cachedResult => tweetCache += (cachedResult.query -> cachedResult.tweets))
      println("Loaded " + tweetCache.keys.toList.length + " cached searches")
    } else {
      println("Cache file does not exist")
    }
  }

  private def writeCache(): Unit = {
    import spark.implicits._
    println("Writing cache to disk")
    val writeableCache = tweetCache.map(cached => TweetSearchResults(cached._1, cached._2)).toList
    writeableCache.toDS().write.mode(SaveMode.Overwrite).save(cacheFile)
    println("Wrote " + writeableCache.length + " saved searches")
  }


  def getTweets(spark: SparkSession, requests: RDD[Product]): Future[Dataset[Product]] = Future {
    import spark.implicits._
    val collectedReq = requests.collect()
    totalRequests = collectedReq.length
    val blah = collectedReq.map(product => {
      while (currentRequests >= maxConcurrentRequests) {
        Thread.sleep(1000)
      }

      val query = product.title.toLowerCase()
      if (tweetCache.keys.exists(_ == query)) {
        requestCount += 1
        Product(product.asin, product.title, tweetCache(query))
      } else {
        searchHandler(query, product)
      }
    })

    // Wait to ensure we get all the responses
    while (requestCount + 1 < totalRequests) {
      println("Waiting for responses, got " + requestCount + " of " + collectedReq.length)
      Thread.sleep(5000)
    }

    // With all of that done, we can build a RDD from this
    spark.createDataFrame(repsonses).as[Product]
  }

  private def getRateLimit(): RateLimit = {
    try {
      Await.result(restClient.rateLimits(), 5.seconds).data.resources.search.values.toList(0)
    } catch {
      case e: Exception => {
        RateLimit(0,0, Instant.now().plus(14, ChronoUnit.MINUTES))
      }
    }
  }

  private def searchHandler(query: String, product: Product): Unit = {
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

        progress()
      }
      case Failure(err: TwitterException) => {
        currentRequests -= 1
        // Refresh the rate limit
        currentRateLimit = getRateLimit()
        // This is caused by a rate limit error, re-execute which will pause until the rate limit is lifted
        searchHandler(query, product)
        progress()
      }
      case Failure(err) => {
        throw err
      }
    }
  }

  private def searchTweet(query: String): Future[RatedData[StatusSearch]] = {
    currentRequests += 1

    Thread.sleep(reqDelay)
    if (currentRateLimit != null) {
      if (currentRateLimit.remaining == 0) {
        val duration = java.time.Duration.between(Instant.now(), currentRateLimit.reset)
        print("\rHit rate limit, pausing for " + duration.toMinutes + " minutes")
        if (duration.toMillis > 0) {
          Thread.sleep(duration.toMillis + 10)
          currentRateLimit = getRateLimit()
          println("Updated ratelimit: " + currentRateLimit.remaining)
          writeCache()
        }
        progress()
      }
    }

    restClient.searchTweet(query)
  }

  private def progress(): Unit = {
    print("\r")
    val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
    print(timestamp + "\tTweetIngest: done " + requestCount + " of " + totalRequests + ", remaining in rate limit " + currentRateLimit.remaining)
  }
}
