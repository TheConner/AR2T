package ca.advtech.ar2t
package entities

case class Product(asin: String, title: String, tweets: List[SimplifiedTweet] = null) extends java.io.Serializable