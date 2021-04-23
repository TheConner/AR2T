package ca.advtech.ar2t
package data

import com.univocity.parsers.csv.{CsvWriter, CsvWriterSettings}
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode}

import java.io.{File, FileOutputStream}
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

class DataWriter(fname: String) {
  private val csvConfig = configuration.getConfigurationClass("data.csvSerializer")
  private val isInputEscaped = csvConfig.getBoolean("isInputEscaped")
  private val quoteAllFields = csvConfig.getBoolean("quoteAllFields")

  def WriteJSON[T: ClassTag](body: Dataset[T]): Unit = {
    body.write.json(fname.concat("_").concat(fname))
  }

  def WriteCSV[T: ClassTag](body: Dataset[T]): Unit = {
    WriteCSV(body.toDF())
  }

  /**
   * Writes a given dataframe to CSV
   * @param body the dataframe to write to the body of the CSV
   */
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

  def WriteDF(dataFrame: DataFrame, name: String): Unit = {
    println("Attempting to write DataFrame to " + fname)
    try {
      dataFrame.write.mode(SaveMode.Overwrite).save(fname.concat("_").concat(name))
      println("DataFrame Written OK")
    } catch {
      case e: Throwable => println(e.getMessage() + '\n' + e.getStackTrace())
    }
  }

  def WriteDS[T](dataSet: Dataset[T], name: String): Unit = {
    println("Attempting to write DataSet to " + fname)
    try {
      dataSet.write.mode(SaveMode.Overwrite).save(fname.concat("_").concat(name))
      println("DataFrame Written OK")
    } catch {
      case e: Throwable => println(e.getMessage() + '\n' + e.getStackTrace())
    }
  }

  def WriteSingleJSON[T](dataSet: Dataset[T], name: String): Unit = {
    println("Attempting to write DataSet to single JSON: " + fname)

    val jsonDS = dataSet.toJSON
    val count = jsonDS.count()

    jsonDS
      .repartition(1)
      .rdd
      .zipWithIndex()
      .map { case(json, idx) =>
        if(idx == 0) "[\n" + json + "," // first row
        else if(idx == count-1) json + "\n]" // last row
        else json + ","
      }
      .saveAsTextFile(fname.concat("_").concat(name))
    println("Dataset written ok")
  }

}
