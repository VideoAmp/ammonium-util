package vamp.ammonium

import java.net.URL
import java.io.{FileInputStream, InputStreamReader, Reader, File}
import java.util.Properties
import scala.collection.JavaConverters._

object SparkProperties {
  private def load(inReader: Reader): Unit =
    getProperties(inReader)
      .filter { case (k, v) => k.startsWith("spark.") }
      .foreach {
        case (k, v) =>
          sys.props.getOrElseUpdate(k, v)
      }

  def load(filename: String): Unit = load(new InputStreamReader(new FileInputStream(filename), "UTF-8"))
  def load(file: File): Unit = load(new InputStreamReader(new FileInputStream(file), "UTF-8"))
  def load(url: URL): Unit = load(new InputStreamReader(url.openStream(), "UTF-8"))

  def loadURL(urlString: String): Unit = load(new URL(urlString))
  def loadURL(url: URL): Unit = load(url)
  def loadFile(filename: String): Unit = load(filename)
  def loadFile(file: File): Unit = load(file)

  private def getProperties(inReader: Reader): Map[String, String] = {
    val properties = new Properties
    try {
      properties.load(inReader)
    }
    finally {
      inReader.close
    }
    properties.stringPropertyNames.asScala.map(k => (k, properties.getProperty(k).trim)).toMap
  }
}
