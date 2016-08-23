import vamp.ammonium.Setup

import java.net.URL

import ammonite.api.{ Classpath, Eval }

object setupURL {
  def apply(url: URL, silent: Boolean = true, silentEval: Boolean = true)(implicit classpath: Classpath, eval: Eval) {
    val setup = new Setup(classpath, eval)
    setup.fromURL(url, silent, silentEval)
  }
}
