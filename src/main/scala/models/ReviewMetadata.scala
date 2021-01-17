package ca.advtech.ar2t
package models

import argonaut._, Argonaut._

case class ReviewMetadata(asin: String, title: String)

object ReviewMetadata {
  implicit def ReviewCodecJson: CodecJson[ReviewMetadata] =
    casecodec2(ReviewMetadata.apply, ReviewMetadata.unapply)("asin", "title")
}