package ca.advtech.ar2t
package Data

import com.univocity.parsers.csv.{CsvWriter, CsvWriterSettings}
import org.apache.spark.sql.DataFrame

import java.io.{File, FileOutputStream}
import scala.collection.JavaConverters._

class DataWriter(fname: String) {
  private val config = configuration.getConfigurationClass("data.csvSerializer")
  private val isInputEscaped = config.getBoolean("isInputEscaped")
  private val quoteAllFields = config.getBoolean("quoteAllFields")

  def WriteCSV(body: DataFrame): Unit = {
    val b = body.collect()
    println("--- Preparing output ---")
    // Create the file
    val f = new File(fname)
    f.createNewFile()
    val oStream = new FileOutputStream(f)
    val settings = new CsvWriterSettings()
    settings.setInputEscaped(isInputEscaped)
    settings.setQuoteAllFields(quoteAllFields)

    val writer = new CsvWriter(oStream, settings)

    b.foreach(row => {
      writer.writeRow(row.toSeq.asJava)
    })

    writer.close()
    println("--- OUTPUT TO " + fname + " COMPLETE ---")
  }

}
