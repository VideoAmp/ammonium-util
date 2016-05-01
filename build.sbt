name := "ammonium-util"

organization := "com.videoamp"

scalaVersion := "2.11.8"

publishTo := {
  val vamp = "https://videoamp.artifactoryonline.com/videoamp/"
  if (isSnapshot.value)
    Some("snapshots" at vamp + "snapshot") 
  else
    Some("releases"  at vamp + "release")
}

val ammoniumVersion = "0.4.0-M6-1"

libraryDependencies ++= Seq(
  "com.github.alexarchambault.ammonium" % s"interpreter-api_${scalaVersion.value}" % ammoniumVersion % "provided",
  "io.get-coursier" %% "coursier" % "1.0.0-M11-1",
  "com.github.kxbmap" %% "configs" % "0.4.2"
)

enablePlugins(BuildInfoPlugin)

buildInfoPackage := "vamp.ammonium"

buildInfoKeys := Seq[BuildInfoKey](
  version,
  "ammoniumVersion" -> ammoniumVersion
)
