scalastyleConfigUrl := Some(url("https://raw.githubusercontent.com/VideoAmp/scalastyle-config/master/scalastyle-config.xml"))

publish <<= publish dependsOn (scalastyle in Compile).toTask("")
