name := "ammonium-util"
organization := "com.videoamp"

scalacOptions := Seq(
  "-deprecation",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-Xlint:_",
  "-Ywarn-adapted-args",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
)

  publishTo := {
    val nexus = "https://videoamp.jfrog.io/videoamp/"
    if (isSnapshot.value) {
      Some("snapshots" at nexus + "snapshot")
    } else {
      Some("releases" at nexus + "release")
    }
  }

  organization := "com.videoamp"

  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

  resolvers += "vamp repo" at "https://videoamp.jfrog.io/videoamp/repo/"

val ammoniumVersion = "0.8.3"

libraryDependencies ++= Seq(
  "org.jupyter-scala" % "ammonite-runtime" % ammoniumVersion % "provided" cross CrossVersion.full,
  // There's a *major* perf regression in hash computations in better-files as of 2.17.1
  // Do *not* upgrade this dep until that perf regression is resolved
  "com.github.pathikrit" %% "better-files" % "2.16.0",
  "io.argonaut"          %% "argonaut"     % "6.2"
)

enablePlugins(BuildInfoPlugin)
buildInfoPackage := "vamp.ammonium"
buildInfoKeys := Seq[BuildInfoKey](
  version,
  "ammoniumVersion" -> ammoniumVersion
)
