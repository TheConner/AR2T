package ca.advtech.ar2t
package entities

import java.time.Instant
import java.util.Date

case class Review(asin: String, reviewerID: String, reviewTime: Instant, reviewText: String)
