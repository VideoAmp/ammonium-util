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
  val vamp = "https://videoamp.artifactoryonline.com/videoamp/"
  if (isSnapshot.value)
    Some("snapshots" at vamp + "snapshot")
  else
    Some("releases" at vamp + "release")
}

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
