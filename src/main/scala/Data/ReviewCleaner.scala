package ca.advtech.ar2t
package Data

import models.Review

import ca.advtech.ar2t.util.StringUtils

import scala.util.matching.Regex

object ReviewCleaner {
  private val config = configuration.getConfigurationClass("dataCleaner")
  private val filterIfNoReviewText = config.getBoolean("filterReviews.ifReviewTextIsNull")
  private val removeParenthesisInTitle = config.getBoolean("cleanMeta.removeParenthesisInTitle")

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

    return true
  }
}
