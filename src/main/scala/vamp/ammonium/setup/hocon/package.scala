package vamp.ammonium.setup

import com.typesafe.config.Config

import configs.{ Configs, ConfigError, Result }
import configs.syntax._

import coursier.Dependency
import coursier.core.{ Parse => CoursierParse, Module, VersionInterval }

package object hocon {
  private implicit val dependencyConfigs: Configs[Dependency] = {
    case class Mod(organization: String, name: String)
    case class Dep(module: Mod, version: String)

    Configs.from { (config, path) =>
      config.get[Dep](path).map { dep =>
        Dependency(Module(dep.module.organization, dep.module.name, Map.empty), dep.version)
      }
    }
  }

  private implicit val versionIntervalConfigs: Configs[VersionInterval] =
    Configs.from { (config, path) =>
      config.get[String](path).flatMap { s =>
        val itvOpt = CoursierParse.ivyLatestSubRevisionInterval(s)
          .orElse(CoursierParse.versionInterval(s))

        itvOpt match {
          case None =>
            Result.failure(ConfigError(s"Cannot parse interval '$s'"))
          case Some(itv) =>
            Result.successful(itv)
        }
      }
    }

  private implicit val scopedDependencyConfigs: Configs[(String, Dependency)] =
    new Configs[(String, Dependency)] {
      override def get(config: Config, path: String): Result[(String, Dependency)] = ???
    }

  def extractSetup(config: Config): Option[Setup] = {
    Option(config.extract[Setup].valueOr { error =>
      error.messages.foreach(println)
      null
    })
  }
}
