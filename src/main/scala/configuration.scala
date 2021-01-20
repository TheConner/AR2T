package ca.advtech.ar2t

import com.typesafe.config.{Config, ConfigFactory}

object configuration {
  val conf = ConfigFactory.load()

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

  def getString(key: String): String = {
    conf.getString(key)
  }

  def getConfigurationClass(className: String): Config = {
    conf.getConfig(className)
  }
}