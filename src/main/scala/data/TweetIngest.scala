package ca.advtech.ar2t
package data

import entities.{Product, SimplifiedTweet}
import util.LoggableDelay

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RateLimit, RatedData, StatusFullSearch}
import com.danielasfregola.twitter4s.exceptions.TwitterException
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}

import java.nio.file.{Files, Paths}
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class TweetIngest(spark: SparkSession) extends Serializable {
  private val config = configuration.getConfigurationClass("data.tweets")
  private val maxConcurrentRequests = config.getInt("maxConcurrentRequests")
  private val restClient = TwitterRestClient(configuration.twitterBearerToken)
  private var tweetCache = scala.collection.mutable.Map[String, List[SimplifiedTweet]]()
  //private var tweetCache: Dataset[TweetSearchResults] = null
  private var responses = ListBuffer[Product]()
  private var currentRateLimit: RateLimit = RateLimit(300, 300, Instant.now().plusSeconds(900))
  private var requestCount = 0
  private var currentRequests = 0
  private var totalRequests = 0
  // Min number of time to pause before requests, in millis
  private val reqDelay = config.getInt("requestDelay")
  private val cacheFile = configuration.getString("data.basePath") + config.getString("writeTweetPath") + config.getString("writeTweetFile")
  private val saveEvery = config.getInt("saveEvery")
  private val fetch = config.getInt("fetch")
  init()

  private def init(): Unit = {
    println("Init TweetIngest")
    loadCache()
  }

  // On create we will want to seed our tweet cache
  def loadCache(): Unit = {
    println("TweetIngest: Loading cache")

    if (Files.exists(Paths.get(cacheFile))) {
      println("Cache file exists")
      val cachedDF = spark.read.parquet(cacheFile)
      cachedDF.printSchema()

      /**
      root
     |-- query: string (nullable = true)
     |-- results: array (nullable = true)
     |    |-- element: struct (containsNull = true)
     |    |    |-- id: long (nullable = true)
     |    |    |-- text: string (nullable = true)
     |    |    |-- author_id: string (nullable = true)
       */
      println("DF size " + cachedDF.count())
      cachedDF.select(col("query"), col("results")).collect().foreach(row => {
        val resArray = row.getList[GenericRowWithSchema](1)
        tweetCache += (row.getString(0) -> resArray.map(str => SimplifiedTweet(str.getLong(0), str.getString(1), str.getString(2))).toList)
      })
      println("Loaded " + tweetCache.keys.toList.length + " cached searches")
    } else {
      println("Cache file does not exist")
    }
  }

  def writeCache(): Unit = {
    import spark.implicits._
    if (tweetCache.keys.toList.isEmpty) {
      return
    }
    println("Writing cache to disk")
    // Build the dataframe
    val dataFrame = tweetCache.toSeq.toDF("query", "results")
    try {
      dataFrame.write.mode(SaveMode.Overwrite).parquet(cacheFile)
      println("DataFrame Written OK")
    } catch {
      case e: Throwable => println(e.getMessage() + '\n' + e.getStackTrace())
    }
    println("Wrote " + tweetCache.toList.length + " saved searches")
  }

  def getTweets(spark: SparkSession, requests: RDD[Product]): Future[Dataset[Product]] = Future {
    import spark.implicits._

    // For sanity testing with things that will have tweets returned
    //val collectedReq = Array(Product("12345", "Hello world"), Product("55555", "Scala"), Product("696969", "Java"))
    val collectedReq = requests.collect()
    totalRequests = collectedReq.length
    println("Total requests to make: " + totalRequests.toString)
    val blah = collectedReq.map(product => {
      while (currentRequests >= maxConcurrentRequests) {
        Thread.sleep(1000)
      }

      val query = product.title.toLowerCase()

      if (query.length < 500) {
        if (tweetCache.keys.exists(_ == query)) {
          requestCount += 1
          responses += Product(product.asin, product.title, tweetCache(query))
        } else {
          searchHandler(query, product)
        }
      } else {
        // Discard, consider it a completed request
        requestCount += 1
      }
    })

    // Wait to ensure we get all the responses
    while (currentRequests > 0)
      LoggableDelay.Delay(5000, f"Waiting to collect all responses, ${currentRequests} remaining")

    writeCache()

    // With all of that done, we can build a RDD from this
    spark.createDataFrame(responses).as[Product]
  }

  def getRestClient(): TwitterRestClient = {
    restClient
  }

  private def searchHandler(query: String, product: Product): Unit = {
    if (requestCount % saveEvery == 0) writeCache()

    val search = searchTweet(query)
    search onComplete {
      case Success(result) => {
        requestCount += 1
        currentRequests -= 1
        currentRateLimit = result.rate_limit
        if (result.data.meta.result_count == 0) {
          // No results :(
          tweetCache += (query -> List())
        } else {
          // When result_count > 0, the data attribute should be populated
          tweetCache += (query -> result.data.data.map(t => SimplifiedTweet(t.id.toLong, t.text, t.author_id.getOrElse(""))))

          responses += Product(product.asin, product.title,
            result.data.data.map(t => SimplifiedTweet(t.id.toLong, t.text, t.author_id.getOrElse(""))))
        }


        progress()
      }
      case Failure(err: TwitterException) => {
        System.err.println("Error fetching tweet: " + err.getMessage)
        System.err.println("Query: " + query)
        currentRequests -= 1
        progress()
      }
      case Failure(err) => {
        throw err
      }
    }
  }

  private def searchTweet(query: String): Future[RatedData[StatusFullSearch]] = {
    currentRequests += 1
    Thread.sleep(reqDelay)
    if (currentRateLimit != null) {
      if (currentRateLimit.remaining == 0) {
        val duration = java.time.Duration.between(Instant.now(), currentRateLimit.reset)
        if (duration.toMillis > 0) {
          LoggableDelay.Delay(duration.toMillis + 10, "Hit rate limit, pausing")
          writeCache()
        }
        progress()
      }
    }
    restClient.searchAllTweet(query,fetch, expansions=Some("author_id"))
  }

  private def progress(): Unit = {
    print("\r")
    val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
    print(timestamp + "\tTweetIngest: done " + requestCount + " of " + totalRequests + ", remaining in rate limit " + currentRateLimit.remaining)
  }
}
