package ca.advtech.ar2t
package entities

import play.api.libs.json.{JsResultException, JsValue, Json}
import sun.jvm.hotspot.HelloWorld.e

import java.time.Instant
import scala.reflect.ClassTag

trait JsonParseable[T] {
  def Parse(s: String): T
}

object JsonParseable {
  def apply[T: JsonParseable]: JsonParseable[T] = implicitly

  implicit object Review extends JsonParseable[Review] {
    override def Parse(s: String): Review = {
      val json: JsValue = Json.parse(s)
      val revTime = (json\"unixReviewTime").as[Long]
      val timeObj = Instant.ofEpochSecond(revTime)
      return new Review(
        (json\"asin").as[String],
        (json\"reviewerID").as[String],
        timeObj,
        if ((json \ "reviewText").asOpt[String].isEmpty) "" else (json\"reviewText").as[String])
    }
  }

  implicit object ReviewMetadata extends JsonParseable[ReviewMetadata] {
    override def Parse(s: String): ReviewMetadata = {
      val json: JsValue = Json.parse(s)
      return new ReviewMetadata(
        (json\"asin").as[String],
        if ( (json \ "title").asOpt[String].isEmpty ) "" else (json\"title").as[String]
      )
    }
  }
}