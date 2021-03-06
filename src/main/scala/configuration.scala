package ca.advtech.ar2t

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.RuntimeConfig

import java.io.File

object configuration {
  private val baseConfig = ConfigFactory.load()
  private val basedir = System.getProperty("user.dir")
  private val myConfigFile = new File(basedir + "/application.conf")
  val twitterBearerToken: String = sys.env.getOrElse("TWITTER_BEARER_TOKEN", throw new RuntimeException("I need a bearer token!"))
  println("Using config source: " + myConfigFile.toURI.toString)
  val conf = ConfigFactory.parseFile(myConfigFile).withFallback(baseConfig)

  // This will throw an exception and crash if the config is malformed
  // it is very intentional :)
  conf.checkValid(ConfigFactory.defaultReference())

  def getPath(entity: String): String = {
    // TODO: use actual path joining here instad of plain strings
    val basePath = conf.getString("data.basePath")
    val dataPath = conf.getString("data." + entity + ".path")
    basePath.concat(dataPath)
  }

  def getMetaPath(entity: String): String = {
    // TODO: use actual path joining here instead of plain strings
    val basePath = conf.getString("data.basePath")
    val metaPath = conf.getString("data." + entity + ".metaPath")
    basePath.concat(metaPath)
  }

  def getRDDPath(entity: String): String = {
    val basePath = conf.getString("data.basePath")
    val rddPath = conf.getString("data.writeRDDPath")
    basePath.concat(rddPath).concat(entity)
  }

  def getString(key: String): String = {
    conf.getString(key)
  }

  def getConfigurationClass(className: String): Config = {
    conf.getConfig(className)
  }

  def configureSpark(runtime: RuntimeConfig) = {
    // Load our spark config class
    val sparkConf = conf.getConfigList("spark.conf")
    sparkConf.forEach(c => {
      val key = c.getString("key")
      val value = c.getString("value")
      println("Setting key " + key + " with value " + value)
      runtime.set(key, value)
    })
  }
}