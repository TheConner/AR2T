package ca.advtech.ar2t
package data

import entities.Product

object MetadataCleaner {

  //region Configuration

  private val config = configuration.getConfigurationClass("data.dataCleaner")
  private val filterLength = config.getInt("cleanMeta.length")
  private val regexes = config.getStringList("cleanMeta.regexClear")
  //endregion

  def filterMetadata(meta: Product): Boolean = {
    if (filterLength > 0 && meta.title.length < filterLength) {
      return false
    }
    return true
  }

  def cleanMetadata(rmIterator: Iterator[Product]): Iterator[Product] = {
    for (meta <- rmIterator) yield cleanMetadataPipeline(meta)
  }

  private def cleanMetadataPipeline(meta: Product): Product = {
    // Clean title
    var title = meta.title
    regexes.forEach(r => title = title.replaceAll(r, ""))

    if (title.equals(meta.title)) {
      // Object hasn't changed
      return meta
    } else {
      // Allocate new obj
      return new Product(meta.asin, title)
    }

  }
}
