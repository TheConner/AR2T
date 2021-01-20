package ca.advtech.ar2t
package util

object StringUtils {
  def isNullOrEmpty[T](s: Seq[T]) = s match {
    case null => true
    case Seq() => true
    case _ => false
  }

  def genUnixTimeFileName(base: String, ext: String): String = {
    val timestamp: Long = System.currentTimeMillis / 1000
    base + timestamp + "." + ext
  }
}
