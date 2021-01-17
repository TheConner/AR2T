package ca.advtech.ar2t

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession

object main {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().
      setMaster("local").
      setAppName("LearnScalaSpark")
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")

    val helloWorldString = "Hello World!"
    print(helloWorldString)
  }

  private def importJsonData(spark: SparkSession): Unit = {

  }

}
