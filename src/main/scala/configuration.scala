package ca.advtech.ar2t

import com.typesafe.config.{Config, ConfigFactory}

object configuration {
  val conf = ConfigFactory.load()


  def getPath(entity: String): String = {
    // TODO: use actual path joining here
    val basePath = conf.getString("data.basePath")
    val dataPath = conf.getString("data." + entity + ".path")
    basePath.concat(dataPath)
  }

  def getMetaPath(entity: String): String = {
    // TODO: use actual path joining here
    val basePath = conf.getString("data.basePath")
    val metaPath = conf.getString("data." + entity + ".metaPath")
    basePath.concat(metaPath)
  }
}