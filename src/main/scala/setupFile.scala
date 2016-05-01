import vamp.ammonium.Setup

import ammonite.api.{ Classpath, Eval }

object setupFile {
  def apply(filename: String, silent: Boolean = true, silentEval: Boolean = true)(implicit classpath: Classpath, eval: Eval) {
    val setup = new Setup(classpath, eval)
    setup.fromFile(filename, silent, silentEval)
  }
}
