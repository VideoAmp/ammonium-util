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

      val expandedEntries =
        entries.flatMap { entry =>
          val expanded =
            if (entry.endsWith("/*")) {
              val dirName = entry.dropRight(1)

              new File(dirName).list.filter(_.endsWith(".jar")).map(dirName + _).sorted
            }
            else {
              Array(entry)
            }

          if (expanded.length > 1) {
            log(s"  $entry (which expands to:)")
            expanded.map("    " + _).foreach(println)
          }
          else {
            log(s"  $entry")
          }

          expanded
        }

      classpath.addPathInConfig(scope)(expandedEntries: _*)
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

    val evalErrors = setup.codePreambles.toSeq.flatMap { codePreambles =>
      codePreambles.flatMap {
        case CodePreamble(name, code) if code.nonEmpty =>
          log(s"Initializing${if (name == "default") "" else s" $name"}")

          val errors = code.flatMap { codeChunk =>
            val optError = if (codeChunk.nonEmpty) {
              val toEval = codeChunk.stripMargin

              log(toEval)

              eval(toEval, silentEval).fold(
                { error =>
                  log("Error: " + error.getClass.getSimpleName + ": " + error.msg)
                  Some(error)
                },
                { _ => None }
              )
            }
            else {
              None
            }

            if (!silentEval) {
              println
            }

            optError
          }

          log()

          errors
        case _ => Nil
      }
    }

    if (evalErrors.isEmpty) {
      if (silent && !silentEval) {
        println
      }

      setup.preamble.foreach { message =>
        println(message.stripMargin)
      }
    }
    else if (silent) {
      println(s"Encountered ${evalErrors.size} evaluation error(s):")
      evalErrors.foreach(error => println(error.getClass.getSimpleName + ": " + error.msg))
    }
  }
}
