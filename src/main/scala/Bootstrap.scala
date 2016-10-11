import vamp.ammonium.setup.{Setup, CodePreamble}
import vamp.ammonium.setup.hocon.extractSetup

import java.net.URL
import com.typesafe.config.ConfigFactory
import ammonite.api.{ Classpath, Eval }

import better.files._

object Bootstrap {
  def apply(masterIP: String,
            port: Int = 8088,
            silent: Boolean = true,
            silentEval: Boolean = true)(
            implicit classpath: Classpath,
            eval: Eval): Unit = {
    val maybePrint: Any => Unit = if (silent) x => () else println
    def logger[T](x: T): T = {maybePrint(x); x}

    val jars = "/tmp/flint/jars".toFile
    jars.createDirectories()

    val serverRoot = new URL("http", masterIP, port, "/")

    val jsons: Iterator[Seq[String]] =
      scala.io.Source.fromURL(new URL(serverRoot, "jars/MANIFEST.json"))
      .getLines()
      .sliding(4)

    logger("Checking manifest and fetching missing jar files...")

    val jarPaths = (for {
      JSON(Jar(name, signature)) <- jsons
      dir = jars/signature
      jarFile = dir/name
    } yield {
      if (!dir.exists()) {
        dir.createDirectory()
        val src = new URL(serverRoot, "jars/" + name).openStream()
        jarFile.writeBytes(
          Iterator.continually(src.read())
          .takeWhile(_ != -1)
          .map(_.toByte))
        logger("Fetched from server:")
      } else {
        logger("Found locally:")
      }

      (logger(jarFile.toString),
       logger(jarFile.sha256.map(_.toLower) == signature))

    }).toList

    val badChecksums: String = jarPaths.collect{
      case (jar, false) => jar
    } match {
      case List() => "\nAll checksums match"
      case ls => ls.mkString("\nBad checksums found:\n", "\n", "")
    }

    val in: Setup = extractSetup(ConfigFactory.parseURL(new URL(serverRoot, "setup/flint"))).get

    val confURL = new URL(serverRoot, "conf/spark-defaults.conf")
    val loadConf = "_root_.vamp.ammonium.SparkProperties.loadURL(\"" + confURL + "\")"
    val setMaster = "sparkConf.setMaster(\"spark://" + masterIP + ":7077\")"
    val extraPreamble = "\nConfigured using " + confURL + badChecksums

    val out: Setup = in.copy(
      classpathEntries = in.classpathEntries.map(_ ++ jarPaths.unzip._1),
      codePreambles = in.codePreambles.map(_.map{
        case CodePreamble("Spark", code) =>
          CodePreamble("Spark", loadConf +: code :+ setMaster)
        case x => x
      }),
      preamble = in.preamble.map(_ + extraPreamble))

    new vamp.ammonium.Setup(classpath, eval).init(out, silent, silentEval)
  }

  private case class Jar(name: String, signature: String)

  private object JSON {
    def unapply(json: Seq[String]): Option[Jar] = json match {
      case Seq("  {", nameLine, signatureLine, "  },") =>
        Some(Jar(nameLine.split("\"")(3),
                 signatureLine.split("\"")(3)))
      case _ => None
    }
  }
}
