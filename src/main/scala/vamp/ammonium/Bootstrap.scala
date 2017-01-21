package vamp.ammonium

import setup.{ Setup => SSetup, CodePreamble }
import setup.hocon.extractSetup

import java.io.File
import java.net.{ InetAddress, URL }

import com.typesafe.config.ConfigFactory

import ammonite.api.{ Classpath, Eval }

import argonaut._, Argonaut._

import better.files.{ File => BFile, _ }

class Bootstrap(cacheDir: String, classpath: Classpath, eval: Eval) {
  import Bootstrap._

  def fromServer(masterAddress: InetAddress, port: Int = 8088, silent: Boolean = true, silentEval: Boolean = true): Unit = {
    val logger: String => String = if (silent) identity else x => { println(x); x }

    val cacheBDir = cacheDir.toFile
    cacheBDir.createDirectories

    val serverRoot = new URL("http", masterAddress.getHostAddress, port, "/")

    val manifest: List[Jar] = new URL(serverRoot, "jars/MANIFEST.json")
      .openStream
      .reader
      .buffered
      .tokens
      .mkString
      .decodeOption[List[Jar]]
      .getOrElse(throw new RuntimeException("Failed to parse manifest at " + serverRoot + "jars/MANIFEST.json"))

    val in: SSetup = extractSetup(ConfigFactory.parseURL(new URL(serverRoot, "setup/flint")))
      .getOrElse(throw new RuntimeException("Failed to extract setup from " + serverRoot + "setup/flint"))

    logger("Checking manifest and fetching missing jar files...")

    val jarPaths: List[(String, Boolean)] = for {
      Jar(name, signature) <- manifest
      jarFile = cacheBDir / signature / name
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
        jarFile.sha256.toLowerCase == signature.toLowerCase
      )
    }

    jarPaths.collect{
      case (jar, false) => jar
    } match {
      case Nil => ()
      case ls => throw new RuntimeException(ls.mkString("Bad checksums found: ", ", ", ""))
    }

    val confURL = new URL(serverRoot, "conf/spark-defaults.conf")
    val loadConf = "_root_.vamp.ammonium.SparkProperties.loadURL(\"" + confURL + "\")"
    val setMaster = "sparkConf.setMaster(\"spark://" + masterAddress.getHostAddress + ":7077\")"
    val extraPreamble = "\nConfigured using " + confURL

    val out: SSetup = in.copy(
      classpathEntries = in.classpathEntries.map(_ ++ jarPaths.map(_._1)),
      codePreambles = in.codePreambles.map(_.map{
        case CodePreamble("Spark", code) =>
          CodePreamble("Spark", loadConf +: code :+ setMaster)
        case x => x
      }),
      preamble = in.preamble.map(_ + extraPreamble)
    )

    new Setup(classpath, eval).init(out, silent, silentEval)
  }
}

object Bootstrap {
  case class Jar(name: String, signature: String)
  implicit def JarDecodeJson: CodecJson[Jar] = CodecJson.derive[Jar]
}
