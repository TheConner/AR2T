package ca.advtech.ar2t
package data

import entities.Review

import ca.advtech.ar2t.util.StringUtils

import java.time.ZoneOffset
import java.time.temporal.{ChronoField, TemporalField}
import java.util.Calendar
import scala.util.matching.Regex

object ReviewCleaner {
  private val config = configuration.getConfigurationClass("data.dataCleaner.filterReviews")
  private val filterIfNoReviewText = config.getBoolean("ifReviewTextIsNull")
  private val minYear = config.getInt("minYear")
  private val maxYear = config.getInt("maxYear")

  /**
   * Determines if we should filter out a given review
   * @param review Review to evaluate
   * @return true if review is OK, false otherwise
   */
  def ReviewFilter(review: Review): Boolean = {
    // Designed to be modular, we can add more filters on in the future
    if (filterIfNoReviewText && StringUtils.isNullOrEmpty(review.reviewText)) {
      return false
    }

    val reviewYear = review.reviewTime.atZone(ZoneOffset.UTC).getYear()
    if (minYear > reviewYear || maxYear < reviewYear) {
      return false
    }

    return true
  }
}
