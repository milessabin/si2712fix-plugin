/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2016, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.annotation

/** An annotation which specifies that when performing higher order
 *  unification type arguments should be captured on the right and
 *  abstracted on the left rather than the default order.
 *
 *  @since 2.12.0
 */
final class unifyRightToLeft extends scala.annotation.StaticAnnotation

