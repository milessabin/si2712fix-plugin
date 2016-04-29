package si2712fix

import scala.collection.mutable

import scala.tools.nsc.{ Global, Phase, SubComponent }
import scala.tools.nsc.plugins.{ Plugin => NscPlugin, PluginComponent => NscPluginComponent }

import scala.tools.nsc.typechecker.MonkeyPatchedAnalyzer

class Plugin(val global: Global) extends NscPlugin {
  import global._
  import scala.reflect.internal.Flags._

  val name = "si2712fix"
  val description = "SI-2712 fix as a compiler plugin"
  val components = Nil

  // install a pretty description for our plugin phase instead of empty string hardcoded for all plugins
  val phasesDescMapGetter = classOf[Global].getDeclaredMethod("phasesDescMap")
  val phasesDescMap = phasesDescMapGetter.invoke(global).asInstanceOf[mutable.Map[SubComponent, String]]
  phasesDescMap(PluginComponent) = "si2712fix"

  // replace Global.analyzer to customize namer and typer (step 1 of 3)
  // unfortunately compiler plugins are instantiated too late
  // therefore by now analyzer has already been used to instantiate the namer, packageobjects and typer subcomponents
  // these are not phases yet - they are just phase factories - so no disaster yet, but we have to be quick
  // this warrants the second step in this customization - rewiring phase factories
  val analyzer = new { val global: Plugin.this.global.type = Plugin.this.global } with MonkeyPatchedAnalyzer
  val analyzerField = classOf[Global].getDeclaredField("analyzer")
  analyzerField.setAccessible(true)
  analyzerField.set(global, analyzer)

  // replace Global.analyzer to customize namer and typer (step 2 of 3)
  // luckily for us compiler plugins are instantiated quite early
  // so by now internal phases have only left a trace in phasesSet and in phasesDescMap
  // also up until now noone has really used the standard analyzer, so we're almost all set
  // except for the standard `object typer extends analyzer.Typer(<some default context>)`
  // that is a member of Global and hence has been pre-initialized now
  // good news is that it's only used in later phases or as a host for less important activities (error reporting, printing, etc)
  val phasesSetMapGetter = classOf[Global].getDeclaredMethod("phasesSet")
  val phasesSet = phasesSetMapGetter.invoke(global).asInstanceOf[mutable.Set[SubComponent]]
  if (phasesSet.exists(_.phaseName == "typer")) { // `scalac -help` doesn't instantiate standard phases
    def subcomponentNamed(name: String) = phasesSet.find(_.phaseName == name).head
    val oldScs @ List(oldNamer, oldPackageobjects, oldTyper) = List(subcomponentNamed("namer"), subcomponentNamed("packageobjects"), subcomponentNamed("typer"))
    val newScs = List(analyzer.namerFactory, analyzer.packageObjects, analyzer.typerFactory)
    def hijackDescription(pt: SubComponent, sc: SubComponent) = phasesDescMap(sc) = phasesDescMap(pt) + " in si2712fix"
    oldScs zip newScs foreach { case (pt, sc) => hijackDescription(pt, sc) }
    phasesSet --= oldScs
    phasesSet ++= newScs
  }

  object PluginComponent extends NscPluginComponent {
    val global = Plugin.this.global
    import global._

    override val runsAfter = List("parser")
    val phaseName = "si2712fix"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {}
    }
  }
}
