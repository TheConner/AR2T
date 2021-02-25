package ca.advtech.ar2t

import com.danielasfregola.twitter4s.TwitterRestClient
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object TestTwitter4s {
  def main(args: Array[String]): Unit = {
    val client = TwitterRestClient()
    val response = client.searchTweet("Scala")
    response onComplete {
      case Success(value) => {
        println(value)
      }
      case Failure(exception) => println(exception)
    }
  }
}
