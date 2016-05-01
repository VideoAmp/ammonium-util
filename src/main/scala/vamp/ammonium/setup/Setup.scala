package vamp.ammonium.setup

import coursier.Dependency
import coursier.core.{ VersionInterval, Version }
import coursier.ivy.Pattern

// Stolen from ammonium.setup.Setup
case class Setup(
    scalaVersion: Option[VersionInterval],
    versions: Option[Map[String, VersionInterval]],
    classpathEntries: Option[Seq[String]],
    dependencies: Option[Seq[Dependency]],
    scopedDependencies: Option[Seq[(String, Dependency)]],
    codePreambles: Option[Seq[CodePreamble]],
    preamble: Option[String]
) {
  def scalaVersionMatches(version: Version): Boolean =
    scalaVersion.forall(_.contains(version))

  def matches(versions0: Map[String, Version]): Boolean =
    versions.getOrElse(Map.empty).forall {
      case (name, itv) =>
        versions0.get(name).forall { v =>
          itv.contains(v)
        }
    }

  private val versionsProperties = versions.fold(Map.empty[String, String]) { m =>
    m.map {
      case (k, itv) => s"$k.version" -> itv.repr
    }
  }

  def classpathEntriesWithProperties(properties: Map[String, String]): Seq[(String, String)] = {
    val properties0 = versionsProperties ++ properties

    def substituteProps(s: String) =
      Pattern.substituteProperties(s, properties0)

    classpathEntries.getOrElse(Nil).map((Setup.defaultConfig, _)).map {
      case (config, entry) => (config, substituteProps(entry))
    }
  }

  def dependenciesWithProperties(properties: Map[String, String]): Seq[(String, Dependency)] = {
    val properties0 = versionsProperties ++ properties

    def substituteProps(s: String) =
      Pattern.substituteProperties(s, properties0)

    val rawDeps = dependencies.getOrElse(Nil).map((Setup.defaultConfig, _)) ++ scopedDependencies.getOrElse(Nil)

    rawDeps.map {
      case (config, rawDep) =>
        val dep = rawDep.copy(
          module = rawDep.module.copy(
            organization = substituteProps(rawDep.module.organization),
            name = substituteProps(rawDep.module.name)
          ),
          version = substituteProps(rawDep.version)
        )

        (config, dep)
    }
  }
}

object Setup {
  private val defaultConfig = "compile"
}
