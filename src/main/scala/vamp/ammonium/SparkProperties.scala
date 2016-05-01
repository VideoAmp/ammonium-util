package vamp.ammonium

object SparkProperties {
  def load(filename: String): Unit =
    getPropertiesFromFile(filename)
      .filter { case (k, v) => k.startsWith("spark.") }
      .foreach {
        case (k, v) =>
          sys.props.getOrElseUpdate(k, v)
      }

  private def getPropertiesFromFile(filename: String): Map[String, String] = {
    import java.io._
    import java.util._
    import scala.collection.JavaConversions._

    val properties = new Properties
    val inReader = new InputStreamReader(new FileInputStream(filename), "UTF-8")

    try {
      properties.load(inReader)
    }
    finally {
      inReader.close
    }

    properties.stringPropertyNames.map(k => (k, properties(k).trim)).toMap
  }
}
