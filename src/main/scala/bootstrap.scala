import vamp.ammonium.setup.{Setup, CodePreamble}
import vamp.ammonium.setup.hocon.extractSetup

import java.net.URL
import com.typesafe.config.ConfigFactory
import ammonite.api.{ Classpath, Eval }

import better.files._
import argonaut._, Argonaut._

object bootstrap {
  case class Jar(name: String, signature: String)
  implicit def JarDecodeJson: DecodeJson[Jar] = jdecode2L(Jar.apply)("name", "signature")

  def apply(masterIP: String,
            port: Int = 8088,
            jarsPath: String = "/tmp/flint/jars",
            silent: Boolean = true,
            silentEval: Boolean = true)(
            implicit classpath: Classpath,
            eval: Eval): Unit = {
    val maybePrint: Any => Unit = if (silent) x => () else println
    def logger[T](x: T): T = {maybePrint(x); x}

    val jars = jarsPath.toFile
    jars.createDirectories()

    val serverRoot = new URL("http", masterIP, port, "/")

    val manifest: List[Jar] =
      scala.io.Source.fromURL(new URL(serverRoot, "jars/MANIFEST.json"))
      .getLines()
      .mkString("\n")
      .decodeOption[List[Jar]]
      .getOrElse(Nil)

    logger("Checking manifest and fetching missing jar files...")

    val jarPaths: List[(String, Boolean)] = for {
      Jar(name, signature) <- manifest
      dir = jars/signature
      jarFile = dir/name
    } yield {
      if (!dir.exists()) {
        dir.createDirectory()
        val src = new URL(serverRoot, "jars/" + name).openStream()
        try {
          jarFile.writeBytes(
            Iterator.continually(src.read())
            .takeWhile(_ != -1)
            .map(_.toByte))
        } finally {
          src.close  
        }
        logger("Fetched from server:")
      } else {
        logger("Found locally:")
      }

      (logger(jarFile.toString),
       logger(jarFile.sha256.map(_.toLower) == signature))
    }

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
}
