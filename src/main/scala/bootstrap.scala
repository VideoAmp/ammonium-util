import vamp.ammonium.Bootstrap

import java.net.InetAddress

import ammonite.api.{ Classpath, Eval }

object bootstrap {
  def apply(
    masterIP: String,
    port: Int = 8088,
    cacheDir: String = "/var/tmp/ammonium/bootstrap",
    silent: Boolean = true,
    silentEval: Boolean = true
  )(implicit classpath: Classpath, eval: Eval): Unit = {
    val bootstrap = new Bootstrap(cacheDir, classpath, eval)
    bootstrap.fromServer(InetAddress.getByName(masterIP), port, silent, silentEval)
  }
}
