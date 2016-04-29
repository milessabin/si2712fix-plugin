package scala.tools.nsc.typechecker {
  import scala.reflect.internal.{ MonkeyPatchedTypes, SymbolTable }

  trait MonkeyPatchedAnalyzer extends Analyzer {
    val tvf = classOf[SymbolTable].getDeclaredField("TypeVar$module")
    tvf.setAccessible(true)

    val ccls = Class.forName("scala.reflect.internal.TypeVarSub")
    val ctor = ccls.getConstructors()(0)
    val mpt = new MonkeyPatchedTypes(global)
    val res = ctor.newInstance(global, mpt.TypeVar)
    tvf.set(global, res)
  }
}

package scala.reflect.internal {
  import scala.tools.nsc.Global

  class MonkeyPatchedTypes(val global: Global) {
    import global._
    import definitions._

    lazy val unifyRightToLeftClass = rootMirror.getClassIfDefined("scala.annotation.unifyRightToLeft")

    trait SI2712Unifier { self: TypeVar =>
      /** A don't care value for the depth parameter in lubs/glbs and related operations. */
      private final val AnyDepth = -3

      /** Called when a TypeVar is involved in a subtyping check.  Result is whether
       *  this TypeVar could plausibly be a [super/sub]type of argument `tp` and if so,
       *  tracks tp as a [lower/upper] bound of this TypeVar.
       *
       *  if (isLowerBound)   this typevar could be a subtype, track tp as a lower bound
       *  if (!isLowerBound)  this typevar could be a supertype, track tp as an upper bound
       *
       *  If isNumericBound is true, the subtype check is performed with weak_<:< instead of <:<.
       */
      override def registerBound(tp: Type, isLowerBound: Boolean, isNumericBound: Boolean = false): Boolean = {
        // println("regBound: "+(safeToString, debugString(tp), isLowerBound)) //@MDEBUG
        if (isLowerBound)
          assert(tp != this)

        // side effect: adds the type to upper or lower bounds
        def addBound(tp: Type) {
          if (isLowerBound) addLoBound(tp, isNumericBound)
          else addHiBound(tp, isNumericBound)
        }
        // swaps the arguments if it's an upper bound
        def checkSubtype(tp1: Type, tp2: Type) = {
          val lhs = if (isLowerBound) tp1 else tp2
          val rhs = if (isLowerBound) tp2 else tp1

          if (isNumericBound) lhs weak_<:< rhs
          else lhs <:< rhs
        }

        /** Simple case: type arguments can be ignored, because either this typevar has
         *  no type parameters, or we are comparing to Any/Nothing.
         *
         *  The latter condition is needed because HK unification is limited to constraints of the shape
         *  {{{
         *    TC1[T1,..., TN] <: TC2[T'1,...,T'N]
         *  }}}
         *  which would preclude the following important constraints:
         *  {{{
         *    Nothing <: ?TC[?T]
         *    ?TC[?T] <: Any
         *  }}}
         */
        def unifySimple = {
          val sym = tp.typeSymbol
          if (sym == NothingClass || sym == AnyClass) { // kind-polymorphic
            // SI-7126 if we register some type alias `T=Any`, we can later end
            // with malformed types like `T[T]` during type inference in
            // `handlePolymorphicCall`. No such problem if we register `Any`.
            addBound(sym.tpe)
            true
          } else if (params.isEmpty) {
            addBound(tp)
            true
          } else false
        }

        /** Full case: involving a check of the form
         *  {{{
         *    TC1[T1,..., TN] <: TC2[T'1,...,T'N]
         *  }}}
         *  Checks subtyping of higher-order type vars, and uses variances as defined in the
         *  type parameter we're trying to infer (the result will be sanity-checked later).
         */
        def unifyFull(tpe: Type): Boolean = {
          def unifySpecific(tp: Type) = {
            if (sameLength(typeArgs, tp.typeArgs)) {
              val lhs = if (isLowerBound) tp.typeArgs else typeArgs
              val rhs = if (isLowerBound) typeArgs else tp.typeArgs
              // This is a higher-kinded type var with same arity as tp.
              // If so (see SI-7517), side effect: adds the type constructor itself as a bound.
              isSubArgs(lhs, rhs, params, AnyDepth) && { addBound(tp.typeConstructor); true }
            } else if(/*settings.YhigherOrderUnification &&*/ typeArgs.lengthCompare(0) > 0 && compareLengths(typeArgs, tp.typeArgs) < 0) {
              // Simple algorithm as suggested by Paul Chiusano in the comments on SI-2712
              //
              //   https://issues.scala-lang.org/browse/SI-2712?focusedCommentId=61270
              //
              // Treat the type constructor as curried and partially applied, we treat a prefix
              // as constants and solve for the suffix. For the example in the ticket, unifying
              // M[A] with Int => Int this unifies as,
              //
              //   M[t] = [t][Int => t]
              //   A = Int
              //
              // A more "natural" unifier might be M[t] = [t][t => t]. There's lots of scope for
              // experimenting with alternatives here.

              val tpSym = tp.typeSymbolDirect
              val rightToLeft = tpSym.annotations.exists(_ matches unifyRightToLeftClass)

              val numAbstracted = typeArgs.length
              val numCaptured = tp.typeArgs.length-numAbstracted
              val (captured, abstracted) =
                if(rightToLeft) tp.typeArgs.splitAt(numAbstracted).swap
               else tp.typeArgs.splitAt(numCaptured)

              val lhs = if (isLowerBound) abstracted else typeArgs
              val rhs = if (isLowerBound) typeArgs else abstracted
              // This is a higher-kinded type var with same arity as tp.
              // If so (see SI-7517), side effect: adds the type constructor itself as a bound.
              isSubArgs(lhs, rhs, params, AnyDepth) && {
                val absSyms =
                  if(rightToLeft) tpSym.typeParams.take(numAbstracted)
                  else tpSym.typeParams.drop(numCaptured)
                val freeSyms = absSyms.map(_.cloneSymbol(tpSym))
                val args =
                  if(rightToLeft) freeSyms.map(_.tpeHK) ++ captured
                  else captured ++ freeSyms.map(_.tpeHK)
                val poly = PolyType(freeSyms, appliedType(tp.typeConstructor, args))
                addBound(poly)
                true
              }
            } else false
          }
          // The type with which we can successfully unify can be hidden
          // behind singleton types and type aliases.
          tpe.dealiasWidenChain exists unifySpecific
        }

        // There's a <: test taking place right now, where tp is a concrete type and this is a typevar
        // attempting to satisfy that test. Either the test will be unsatisfiable, in which case
        // registerBound will return false; or the upper or lower bounds of this type var will be
        // supplemented with the type being tested against.
        //
        // Eventually the types which have accumulated in the upper and lower bounds will be lubbed
        // (resp. glbbed) to instantiate the typevar.
        //
        // The only types which are eligible for unification are those with the same number of
        // typeArgs as this typevar, or Any/Nothing, which are kind-polymorphic. For the upper bound,
        // any parent or base type of `tp` may be tested here (leading to a corresponding relaxation
        // in the upper bound.) The universe of possible glbs, being somewhat more infinite, is not
        // addressed here: all lower bounds are retained and their intersection calculated when the
        // bounds are solved.
        //
        // In a side-effect free universe, checking tp and tp.parents beofre checking tp.baseTypeSeq
        // would be pointless. In this case, each check we perform causes us to lose specificity: in
        // the end the best we'll do is the least specific type we tested against, since the typevar
        // does not see these checks as "probes" but as requirements to fulfill.
        // TODO: can the `suspended` flag be used to poke around without leaving a trace?
        //
        // So the strategy used here is to test first the type, then the direct parents, and finally
        // to fall back on the individual base types. This warrants eventual re-examination.

        // AM: I think we could use the `suspended` flag to avoid side-effecting during unification
        if (suspended)         // constraint accumulation is disabled
          checkSubtype(tp, origin)
        else if (constr.instValid)  // type var is already set
          checkSubtype(tp, constr.inst)
        else isRelatable(tp) && {
          unifySimple || unifyFull(tp) || (
            // only look harder if our gaze is oriented toward Any
            isLowerBound && (
              (tp.parents exists unifyFull) || (
                // @PP: Is it going to be faster to filter out the parents we just checked?
                // That's what's done here but I'm not sure it matters.
                tp.baseTypeSeq.toList.tail filterNot (tp.parents contains _) exists unifyFull
              )
            )
          )
        }
      }

      val sm = classOf[TypeVar].getDeclaredMethod("scala$reflect$internal$Types$$suspended")
      sm.setAccessible(true)
      def suspended: Boolean = sm.invoke(this).asInstanceOf[Boolean]
    }

    val tm = classOf[Types].getDeclaredMethod("scala$reflect$internal$Types$$traceTypeVars")
    tm.setAccessible(true)
    def traceTypeVars: Boolean = tm.invoke(global).asInstanceOf[Boolean]

    val pm = classOf[Types].getDeclaredMethod("scala$reflect$internal$Types$$propagateParameterBoundsToTypeVars")
    pm.setAccessible(true)
    def propagateParameterBoundsToTypeVars: Boolean = pm.invoke(global).asInstanceOf[Boolean]

    //@M
    // a TypeVar used to be a case class with only an origin and a constr
    // then, constr became mutable (to support UndoLog, I guess),
    // but pattern-matching returned the original constr0 (a bug)
    // now, pattern-matching returns the most recent constr
    object TypeVar {
      @inline final def trace[T](action: String, msg: => String)(value: T): T = {
        if (traceTypeVars) {
          val s = msg match {
            case ""   => ""
            case str  => "( " + str + " )"
          }
          Console.err.println("[%10s] %-25s%s".format(action, value, s))
        }
        value
      }

      /** Create a new TypeConstraint based on the given symbol.
       */
      private def deriveConstraint(tparam: Symbol): TypeConstraint = {
        /** Must force the type parameter's info at this point
         *  or things don't end well for higher-order type params.
         *  See SI-5359.
         */
        val bounds  = tparam.info.bounds
        /* We can seed the type constraint with the type parameter
         * bounds as long as the types are concrete.  This should lower
         * the complexity of the search even if it doesn't improve
         * any results.
         */
        if (propagateParameterBoundsToTypeVars) {
          val exclude = bounds.isEmptyBounds || (bounds exists typeIsNonClassType)

          if (exclude) new TypeConstraint
          else TypeVar.trace("constraint", "For " + tparam.fullLocationString)(new TypeConstraint(bounds))
        }
        else new TypeConstraint
      }
      def untouchable(tparam: Symbol): TypeVar                 = createTypeVar(tparam, untouchable = true)
      def apply(tparam: Symbol): TypeVar                       = createTypeVar(tparam, untouchable = false)
      def apply(origin: Type, constr: TypeConstraint): TypeVar = apply(origin, constr, Nil, Nil)
      def apply(origin: Type, constr: TypeConstraint, args: List[Type], params: List[Symbol]): TypeVar =
        createTypeVar(origin, constr, args, params, untouchable = false)

      /** This is the only place TypeVars should be instantiated.
       */
      def createTypeVar(origin: Type, constr: TypeConstraint, args: List[Type], params: List[Symbol], untouchable: Boolean): TypeVar = {
        val tv = (
          if (args.isEmpty && params.isEmpty) {
            if (untouchable) new TypeVar(origin, constr) with UntouchableTypeVar with SI2712Unifier
            else new TypeVar(origin, constr) with SI2712Unifier
          }
          else if (args.size == params.size) {
            if (untouchable) new AppliedTypeVar(origin, constr, params zip args) with UntouchableTypeVar with SI2712Unifier
            else new AppliedTypeVar(origin, constr, params zip args) with SI2712Unifier
          }
          else if (args.isEmpty) {
            if (untouchable) new HKTypeVar(origin, constr, params) with UntouchableTypeVar with SI2712Unifier
            else new HKTypeVar(origin, constr, params) with SI2712Unifier
          }
          else throw new Error("Invalid TypeVar construction: " + ((origin, constr, args, params)))
        )

        trace("create", "In " + tv.originLocation)(tv)
      }
      def createTypeVar(tparam: Symbol, untouchable: Boolean): TypeVar =
        createTypeVar(tparam.tpeHK, deriveConstraint(tparam), Nil, tparam.typeParams, untouchable)
    }
  }
}
