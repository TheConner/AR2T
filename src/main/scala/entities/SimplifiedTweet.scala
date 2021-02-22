package ca.advtech.ar2t
package entities

import play.api.libs.json.JsValue

import java.time.Instant

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
case class SimplifiedTweet(created_at: Instant,
                           favourite_count: Int = 0,
                           id: Long,
                           lang: Option[String] = None,
                           retweet_count: Long = 0,
                           source: String,
                           text: String,
                           truncated: Boolean = false
                          )



