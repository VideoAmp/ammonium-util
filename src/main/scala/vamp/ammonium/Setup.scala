package vamp.ammonium

import setup.{ CodePreamble, Setup => SSetup }

import java.io.File

import ammonite.api.{ Classpath, Eval }

class Setup(classpath: Classpath, eval: Eval) {
  def fromFile(filename: String, silent: Boolean, silentEval: Boolean) {
    import setup.hocon._

    val file = new File(filename)

    require(file.exists, s"No file at $filename")
    require(file.isFile, s"Not a regular file: $filename")
    require(file.canRead, s"Not readable: $filename")

    val setupOpt = parseSetupFile(new File(filename))
    setupOpt.foreach { setup =>
      init(filename, setup, silent, silentEval)
    }
  }

  // Stolen from ammonite.util.Setup
  def init(name: String, setup: SSetup, silent: Boolean, silentEval: Boolean) {
    def log(message: String = ""): Unit = if (!silent) { println(message) }

    val properties = Map(
      "scala.version" -> scala.util.Properties.versionNumberString,
      "scala.binaryVersion" -> scala.util.Properties.versionNumberString.split('.').take(2).mkString(".")
    ) ++ Map("ammonium.version" -> BuildInfo.ammoniumVersion)

    val classpathEntries = setup.classpathEntriesWithProperties(properties)

    val classpathEntriesByScope =
      classpathEntries.groupBy {
        case (scope, _) => scope
      }.map {
        case (scope, value) => scope -> value.map(_._2)
      }

    for ((scope, entries) <- classpathEntriesByScope) {
      log(s"Adding classpath entries${if (scope == "compile") "" else s" in configuration $scope"}")

      for (entry <- entries) {
        log(s"  $entry")
      }

      classpath.addPathInConfig(scope)(entries: _*)
      log()
    }

    val dependencies = setup.dependenciesWithProperties(properties)

    val dependencies0 = dependencies.map {
      case (scope, dep) =>
        scope -> ((dep.module.organization, dep.module.name, dep.version))
    }

    val g = dependencies0.groupBy {
      case (scope, _) => scope
    }.map {
      case (scope, v) =>
        scope -> v.map {
          case (_, d) => d
        }
    }

    // FIXME No roll-back if the dependencies of a given scope cannot be found here
    for ((scope, deps) <- g) {
      log(s"Adding dependencies${if (scope == "compile") "" else s" in configuration $scope"}")

      for ((org, name, ver) <- deps.sorted)
        log(s"  $org:$name:$ver")

      classpath.addInConfig(scope)(deps: _*)
      log()
    }

    val codePreambles = setup.codePreambles.toSeq

    for (codePreambles <- setup.codePreambles) {
      for (CodePreamble(mod, code) <- codePreambles if code.nonEmpty) {
        log(s"Initializing${if (mod == "default") "" else s" $mod"}")

        code.map(_.replaceAll("\n", ""))
        val lines = code.flatMap(_.split("\n", -1))

        for (codeChunk <- code) {
          if (codeChunk.nonEmpty) {
            val stripped = codeChunk.stripMargin

            log(stripped.split("\n", -1).map("  " + _).mkString("\n"))
            eval(stripped.replaceAll("\n", ""), silentEval)
          }

          if (!silentEval) {
            log()
          }
        }

        log()
      }
    }

    setup.preamble.foreach { message =>
      println(message.stripMargin)
    }
  }
}
