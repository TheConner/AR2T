package ca.advtech.ar2t
package Data

import models.ReviewMetadata

object MetadataCleaner {
  private val config = configuration.getConfigurationClass("dataCleaner")
  private val filterLength = config.getInt("cleanMeta.length")
  private val regexes = config.getStringList("cleanMeta.regexClear")

  def filterMetadata(meta: ReviewMetadata): Boolean = {
    if (filterLength > 0 && meta.title.length < filterLength) {
      return false
    }
    return true
  }

  def cleanseMetadata(rmIterator: Iterator[ReviewMetadata]): Iterator[ReviewMetadata] = {
    for (meta <- rmIterator) yield cleansePipeline(meta)
  }

  private def cleansePipeline(meta: ReviewMetadata): ReviewMetadata = {
    // Clean title
    var title = meta.title
    regexes.forEach(r => title = title.replaceAll(r, ""))

    if (title.equals(meta.title)) {
      // Object hasn't changed
      return meta
    } else {
      // Allocate new obj
      return new ReviewMetadata(meta.asin, title)
    }

  }
}
