# VideoAmp's Ammonium Utils

[![Travis](https://img.shields.io/travis/VideoAmp/ammonium-util.svg)](https://travis-ci.org/VideoAmp/ammonium-util)
[![Maven Central](https://img.shields.io/maven-central/v/com.videoamp/ammonium-util_2.11.svg)](https://repo1.maven.org/maven2/com/videoamp/ammonium-util_2.11/)

These utilities consist principally of a bootstrapping system for connecting to a [Flint](https://github.com/VideoAmp/flint) Spark cluster from an [Ammonium](https://github.com/jupyter-scala/ammonium) shell or [Jupyter Scala](https://github.com/jupyter-scala/jupyter-scala) notebook.

## Usage

Assuming you have a Flint Spark master running at ip address `masterIP`, run

```scala
interp.load.ivy("com.videoamp" %% "ammonium-util" % "2.0.2")
# dc3
vamp.ammonium.bootstrap(masterIP, env="dc3")
# use1
vamp.ammonium.bootstrap(masterIP, env="use1")
```

in a Jupyter Scala notebook to bind a `SparkConf` to the `sparkConf` variable in your notebook environment. After configuring Spark through `sparkConf`, you can access a bootstrapped and configured `SparkSession` with the `spark` notebook variable.
