import vamp.ammonium.setup.{ Setup, CodePreamble }
import vamp.ammonium.setup.hocon.extractSetup

import java.net.URL
import com.typesafe.config.ConfigFactory
import ammonite.api.{ Classpath, Eval }

import better.files._
import argonaut._, Argonaut._

object bootstrap {
  case class Jar(name: String, signature: String)
  implicit def JarDecodeJson: CodecJson[Jar] = CodecJson.derive[Jar]

  def apply(
    masterIP: String,
    port: Int = 8088,
    jarsPath: String = "/var/tmp/ammonium/bootstrap",
    silent: Boolean = true,
    silentEval: Boolean = true
  )(
    implicit
    classpath: Classpath,
    eval: Eval
  ): Unit = {
    val logger: String => String = if (silent) identity else x => { println(x); x }

    val jarsDir = jarsPath.toFile
    jarsDir.createDirectories

    val serverRoot = new URL("http", masterIP, port, "/")

    val manifest: List[Jar] = new URL(serverRoot, "jars/MANIFEST.json")
      .openStream
      .reader
      .buffered
      .tokens
      .mkString
      .decodeOption[List[Jar]]
      .getOrElse(throw new java.lang.RuntimeException("Failed to parse manifest at " + serverRoot + "jars/MANIFEST.json"))

    val in: Setup = extractSetup(ConfigFactory.parseURL(new URL(serverRoot, "setup/flint")))
      .getOrElse(throw new java.lang.RuntimeException("Failed to extract setup from " + serverRoot + "setup/flint"))

    logger("Checking manifest and fetching missing jar files...")

    val jarPaths: List[(String, Boolean)] = for {
      Jar(name, signature) <- manifest
      jarFile = jarsDir / signature / name
    } yield {
      if (!jarFile.exists) {
        jarFile.createIfNotExists(asDirectory = false, createParents = true)
        new URL(serverRoot, "jars/" + name).openStream > jarFile.newOutputStream
        logger("Fetched from server:")
      }
      else {
        logger("Found locally:")
      }
      (
        logger(jarFile.toString),
        jarFile.sha256.map(_.toLower) == signature
      )
    }

    val badChecksums: String = jarPaths.collect{
      case (jar, false) => jar
    } match {
      case Nil => "\nAll checksums match"
      case ls => ls.mkString("\nBad checksums found:\n", "\n", "")
    }

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
      preamble = in.preamble.map(_ + extraPreamble)
    )

    new vamp.ammonium.Setup(classpath, eval).init(out, silent, silentEval)
  }
}
