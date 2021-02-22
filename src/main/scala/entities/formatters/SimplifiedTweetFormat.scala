package ca.advtech.ar2t
package entities.formatters
import entities.SimplifiedTweet

import play.api.libs.json._

object SimplifiedTweetFormat {
  implicit val SimplifiedTweetFormatReader = Json.writes[SimplifiedTweet]
  implicit val SimplifiedTweetFormatWriter = Json.reads[SimplifiedTweet]
}
