package si2712fix

import scala.reflect.internal.{ MonkeyPatchedTypes, SymbolTable }
import scala.tools.nsc.{ Global, Phase, SubComponent }
import scala.tools.nsc.plugins.{ Plugin => NscPlugin }

class Plugin(val global: Global) extends NscPlugin {
  import global._

  val name = "si2712fix"
  val description = "SI-2712 fix as a compiler plugin"
  val components = Nil

  val tvf = classOf[SymbolTable].getDeclaredField("TypeVar$module")
  tvf.setAccessible(true)

  val ccls = Class.forName("scala.reflect.internal.TypeVarSub")
  val ctor = ccls.getConstructors()(0)
  val mpt = new MonkeyPatchedTypes(global)
  val res = ctor.newInstance(global, mpt.TypeVar)
  tvf.set(global, res)
}
