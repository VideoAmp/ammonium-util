package vamp.ammonium

import java.net.URL
import java.io.{InputStreamReader, Reader, File}
import java.util.Properties
import scala.collection.JavaConverters._

import better.files.{File => SFile, _}

object SparkProperties {
  def load(filename: String): Unit = loadFile(filename)

  def loadFile(filename: String): Unit = loadFile(filename.toFile)
  def loadFile(file: File): Unit = loadFile(file.toScala)
  def loadFile(file: SFile): Unit = {
    val inReader = file.newInputStream.reader
    try {
      load(inReader)
    } finally {
      inReader.close
    }
  }

  def loadURL(urlString: String): Unit = loadURL(new URL(urlString))
  def loadURL(url: URL): Unit = {
    val inReader = new InputStreamReader(url.openStream(), "UTF-8")
    try {
      load(inReader)
    } finally {
      inReader.close
    }
  }

  private def load(inReader: Reader): Unit =
    for {
      (k, v) <- getProperties(inReader)
      if k.startsWith("spark.")
    } sys.props.getOrElseUpdate(k, v)

  private def getProperties(inReader: Reader): Map[String, String] = {
    val properties = new Properties
    properties.load(inReader)
    properties.stringPropertyNames.asScala.map(k => (k, properties.getProperty(k).trim)).toMap
  }
}
