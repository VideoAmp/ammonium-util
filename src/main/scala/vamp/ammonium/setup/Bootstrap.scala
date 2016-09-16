package vamp.ammonium.setup

import java.net.URL
import java.io.{File, FileOutputStream, InputStream}
import scala.annotation.tailrec
import hocon.extractSetup
import com.typesafe.config.ConfigFactory
import ammonite.api.{ Classpath, Eval }

object Bootstrap {
  def apply(masterIP: String,
            port: Int = 8088,
            silent: Boolean = true,
            silentEval: Boolean = true)(
            implicit classpath: Classpath,
            eval: Eval): Unit = {
    val logger = logMaker(silent)

    val jars = new File("/tmp/flint/jars")
    jars.mkdirs()

    val serverRoot = new URL("http", masterIP, port, "/")

    val jsons: Iterator[Seq[String]] =
      scala.io.Source.fromInputStream(
        new URL(serverRoot, "jars/MANIFEST.json")
        .openStream())
      .getLines()
      .sliding(4)

    val jarPaths: Iterator[String] = for {
      JSON(Jar(name, signature)) <- jsons
      dir = new File(jars, signature)
      jarFile = new File(dir, name)
    } yield {
        logger(
          if (dir.mkdir()) {
            ioLoop(new URL(serverRoot, "jars/" + name).openStream(),
                   new FileOutputStream(jarFile))
            "Fetched from server:"
          } else "Found locally:")
        logger(jarFile.toString)
    }

    val in: Setup = extractSetup(ConfigFactory.parseURL(new URL(serverRoot, "setup/flint"))).get

    val confURL = new URL(serverRoot, "conf/spark-defaults.conf")
    val loadConf = "_root_.vamp.ammonium.SparkProperties.loadURL(\"" + confURL + "\")"
    val setMaster = "sparkConf.setMaster(\"spark://" + masterIP + ":7077\")"
    val extraPreamble = "\nConfigured using " + confURL

    logger("Checking manifest and fetching missing jar files...")

    val out: Setup = in.copy(
      classpathEntries = in.classpathEntries.map(_ ++ jarPaths),
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

  private def ioLoop(input: InputStream, output: FileOutputStream): Unit = {
    @tailrec
    def loop: Unit = {
      val byte = input.read()
      if (byte != -1) {
        output.write(byte)
        loop
      }
    }
    try {
      loop
    } finally {
      input.close()
      output.close()
    }
  }

  private def logMaker(silent: Boolean): String => String =
    if (silent) x => x
    else x => {println(x); x}
}
