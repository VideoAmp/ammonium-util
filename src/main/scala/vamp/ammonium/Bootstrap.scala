package vamp.ammonium

import java.net.{ InetAddress, URL }
import java.io.{ Reader, File => JFile }
import java.util.Properties

import scala.collection.JavaConverters._

import ammonite.ops.Path
import ammonite.runtime.InterpAPI

import argonaut._, Argonaut._

import better.files._

class Bootstrap(cacheDir: String, interp: InterpAPI) {
  import Bootstrap._

  def fromServer(
      masterAddress: InetAddress,
      port: Int = 8088,
      silent: Boolean = true,
      silentEval: Boolean = true
  ): Unit = {
    val logger: String => String = if (silent) identity else x => { println(x); x }

    val cacheBDir = cacheDir.toFile
    cacheBDir.createDirectories

    val serverRoot = new URL("http", masterAddress.getHostAddress, port, "/")

    val manifest: List[Jar] =
      new URL(serverRoot, "jars/MANIFEST.json").openStream.reader.buffered.tokens.mkString
        .decodeOption[List[Jar]]
        .getOrElse(throw new RuntimeException(
          "Failed to parse manifest at " + serverRoot + "jars/MANIFEST.json"))

    logger("Checking manifest and fetching missing jar files...")

    val jarPaths: List[(String, Boolean)] = for {
      Jar(name, signature) <- manifest
      jarFile = cacheBDir / signature / name
    } yield {
      if (!jarFile.exists) {
        jarFile.createIfNotExists(asDirectory = false, createParents = true)
        new URL(serverRoot, "jars/" + name).openStream > jarFile.newOutputStream
        logger("Fetched from server:")
      } else {
        logger("Found locally:")
      }
      (logger(jarFile.toString), jarFile.sha256.toLowerCase == signature.toLowerCase)
    }

    jarPaths.collect {
      case (jar, false) => jar
    } match {
      case Nil => ()
      case ls  => throw new RuntimeException(ls.mkString("Bad checksums found: ", ", ", ""))
    }

    logger(s"Adding ${jarPaths.size} jars to classpath")
    interp.load.cp(jarPaths.map(_._1).map(Path(_)))

    val confUrl = new URL(serverRoot, "conf/spark-defaults.conf")
    SparkProperties.loadURL(confUrl)

    logger("")
    logger("Initializing Spark")

    def logAndEval(code: String): Unit = {
      logger(code)
      interp.load(code, silentEval)
    }

    val bootstrapScript =
      new URL(serverRoot, "bootstrap.sc").openStream.lines
        .mkString("\n")
    logAndEval(bootstrapScript)
    logger("")

    logAndEval(s"""sparkConf.setMaster("spark://${masterAddress.getHostAddress}:7077")""")

    val addJars = s"""
      val cacheBDir = better.files.File("$cacheDir")
      sparkConf.set("spark.jars",
        kernel
          .sess
          .frames
          .flatMap(_.classpath)
          .filter(f =>
            f.isFile &&
              f.getName.endsWith(".jar") &&
              !cacheBDir.contains(better.files.File(f.getPath)))
          .map(_.toURI)
          .mkString(","))""".split("\n", -1).map(_.replaceFirst("      ", "")).mkString("\n")
    logAndEval(addJars)
    logger("")

    println(s"Configured using $confUrl")
    println(
      "Adjust the Spark config via `sparkConf`. Then access the `SparkSession` at `spark` or the `SparkContext` at `sc`.")
  }
}

object Bootstrap {
  case class Jar(name: String, signature: String)
  implicit def JarDecodeJson: CodecJson[Jar] = CodecJson.derive[Jar]
}
