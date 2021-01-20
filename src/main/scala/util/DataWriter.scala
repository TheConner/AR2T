package ca.advtech.ar2t
package util

import com.univocity.parsers.csv.{CsvWriter, CsvWriterSettings}
import org.apache.spark.sql.{DataFrame, Row}

import java.io.{File, FileOutputStream}
import scala.collection.JavaConverters._
import scala.collection.mutable

class DataWriter(fname: String) {

  def WriteCSV(body: DataFrame): Unit = {
    // Create the file
    val f = new File(fname)
    f.createNewFile()
    val oStream = new FileOutputStream(f)
    val writer = new CsvWriter(oStream, new CsvWriterSettings())

    body.collect().foreach(row => {
      writer.writeRow(row.toSeq.asJava)
    })

    writer.close()
    println("--- OUTPUT TO " + fname + " COMPLETE ---")
  }

}
