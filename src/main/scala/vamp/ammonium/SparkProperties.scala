package vamp.ammonium

import java.net.URL
import java.io.{InputStreamReader, Reader, File}
import java.util.Properties
import scala.collection.JavaConverters._

import better.files.{File => SFile, _}

object SparkProperties {
  private def load(inReader: Reader): Unit =
    for {
      (k, v) <- getProperties(inReader)
      if k.startsWith("spark.")
    } sys.props.getOrElseUpdate(k, v)

  def load(filename: String): Unit = loadFile(filename)

  def loadFile(filename: String): Unit = loadFile(filename.toFile)
  def loadFile(file: File): Unit = loadFile(file.toScala)
  def loadFile(file: SFile): Unit = load(file.newInputStream.reader)

  def loadURL(urlString: String): Unit = loadURL(new URL(urlString))
  def loadURL(url: URL): Unit = load(new InputStreamReader(url.openStream(), "UTF-8"))

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
