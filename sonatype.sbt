publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/VideoAmp/ammonium-util"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/VideoAmp/ammonium-util"),
    "scm:git@github.com:VideoAmp/ammonium-util.git"
  )
)
developers := List(
  Developer(
    id = "mallman",
    name = "Michael Allman",
    email = "msa@allman.ms",
    url = url("https://github.com/mallman"))
)
