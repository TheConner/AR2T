package ca.advtech.ar2t
package entities

/**
 * Simplified tweet class
 * @param created_at
 * @param favourite_count
 * @param id
 * @param lang
 * @param retweet_count
 * @param source
 * @param text
 * @param truncated
 */
@SerialVersionUID(140L)
case class SimplifiedTweet(
                           id: Long,
                           text: String,
                           author_id: String
                          )



