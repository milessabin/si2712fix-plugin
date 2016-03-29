package test

import scala.annotation.unifyRightToLeft

// Cats Xor, Scalaz \/, scala.util.Either
sealed abstract class Xor[+A, +B] extends Product with Serializable
object Xor {
  final case class Left[+A](a: A) extends (A Xor Nothing)
  final case class Right[+B](b: B) extends (Nothing Xor B)
}

object Test7 {
	import Xor._
  def meh[F[_], A, B](fa: F[A])(f: A => B): F[B] = ???
  meh(new Right(23): Xor[Boolean, Int])(_ < 13)
  meh(new Left(true): Xor[Boolean, Int])(_ < 13)
}

// Scalactic Or
@unifyRightToLeft
sealed abstract class Or[+G,+B]
final case class Good[+G](g: G) extends Or[G,Nothing]
final case class Bad[+B](b: B) extends Or[Nothing,B]

object TestOr {
  def meh[F[_], A, B](fa: F[A])(f: A => B): F[B] = ???
  meh(new Good(23): Or[Int, Boolean])(_ < 13)
  meh(new Bad(true): Or[Int, Boolean])(_ < 13)
}
