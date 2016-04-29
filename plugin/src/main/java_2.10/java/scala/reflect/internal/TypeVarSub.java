package scala.reflect.internal;

import scala.collection.immutable.List;
import scala.collection.immutable.Nil$;

import scala.reflect.internal.Types;

public class TypeVarSub extends Types.TypeVar$ {
  MonkeyPatchedTypes.TypeVar$ delegate;

  public TypeVarSub(SymbolTable outer, MonkeyPatchedTypes.TypeVar$ delegate0) {
    // Note that the superclass constructor is miscompiled by scalac so
    // this call has to be tweaked at the bytecode level to prevent it from
    // failing at runtime.
    outer.super();
    delegate = delegate0;
  }

  public Types.TypeVar untouchable(Symbols.Symbol tparam) {
    return delegate.createTypeVar(tparam, true);
  }

  public Types.TypeVar apply(Symbols.Symbol tparam) {
    return delegate.createTypeVar(tparam, false);
  }

  public Types.TypeVar apply(Types.Type origin, Types.TypeConstraint constr) {
    return delegate.apply(origin, constr, (List<Types.Type>)(Object)Nil$.MODULE$, (List<Symbols.Symbol>)(Object)Nil$.MODULE$);
  }

  public Types.TypeVar apply(Types.Type origin, Types.TypeConstraint constr, List<Types.Type> args, List<Symbols.Symbol> params) {
    return delegate.createTypeVar(origin, constr, args, params, false);
  }
}
