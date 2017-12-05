package vamp

import java.net.InetAddress

import ammonite.runtime.InterpAPI

package object ammonium {
  def bootstrap(
      masterIP: String,
      port: Int = 8088,
      cacheDir: String = "/var/tmp/ammonium/bootstrap",
      silent: Boolean = true,
      silentEval: Boolean = true,
      env: String = "use1") (implicit interp: InterpAPI): Unit = {
    val bootstrap = new Bootstrap(cacheDir, interp)
    bootstrap.fromServer(InetAddress.getByName(masterIP), port, silent, silentEval, env)
  }
}
