package com.digischool.kkp.dsl2.utils.validation

import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

trait Applicative[M[+_]] {
  def unit[A](a: A): M[A]
  def ap[A, B](fa: M[A], fab: M[A => B]): M[B]

  def map[A, B](fa: M[A], f: A => B): M[B] = ap(fa, unit(f))

  def zipWith[A, B, C](fa: M[A], fb: M[B])(f: (A, B) => C): M[C] =
    ap(fa, map(fb, (b: B) => (a: A) => f(a, b)))

  def zip[A, B](fa: M[A], fb: M[B]): M[(A, B)] = zipWith(fa, fb)((_, _))

  implicit class AppOps[A](fa: M[A]) {
    def |->[B](f: A => B) = map(fa, f)
    def |@|[B](fab: M[A => B]) = ap(fa, fab)
    def /^/[B, C](fb: M[B]): ((A, B) => C) => M[C] = zipWith[A, B, C](fa, fb)
  }
}

object Applicative {
  def apply[T[+_]](implicit T: Applicative[T]) = T

  type Id[+T] = T

  implicit val idApp: Applicative[({type F[+T] = T})#F] = new Applicative[({type F[+T] = T})#F] {
    override def ap[A, B](fa: Id[A], fab: Id[(A) => B]): Id[B] = fab(fa)
    override def unit[A](a: A): Id[A] = a
  }

  implicit val tryApp: Applicative[Try] = new Applicative[Try] {
    // no need to pretend that Try is a real applicative, might as well catch as much errors as possible
    override def unit[A](a: A): Try[A] = Try(a)

    override def ap[A, B](fa: Try[A], fab: Try[(A) => B]): Try[B] = for {
      a <- fa
      f <- fab
    } yield f(a)
  }

  implicit val validationApp: Applicative[Validation] = new Applicative[Validation] {
    override def ap[A, B](fa: Validation[A], fab: Validation[(A) => B]): Validation[B] = (fa, fab) match {
      case (Valid(a), Valid(f)) => Valid(f(a))
      case (Invalid(l), Invalid(ll)) => Invalid(l ++ ll)
      case (i: Invalid, _) => i
      case (_, i: Invalid) => i
    }
    override def unit[A](a: A): Validation[A] = Valid(a)
  }

  implicit def futureApp(implicit context: ExecutionContext): Applicative[Future] = new Applicative[Future] {
    override def ap[A, B](fa: Future[A], fab: Future[(A) => B]): Future[B] = fa zip fab map {
      case (a, f) => f(a)
    }
    override def unit[A](a: A): Future[A] = Future(a)
  }
  
  implicit def compose[F[+_], G[+_]](implicit F: Applicative[F], G: Applicative[G]): Applicative[({type C[+X] = F[G[X]]})#C] =
    new Applicative[({type C[+X] = F[G[X]]})#C] {
      type C[A] = F[G[A]]

      override def unit[A](a: A): C[A] = F.unit(G.unit(a))

      override def ap[A, B](fga: C[A], fgab: C[(A) => B]): C[B] =
        F.ap[G[A], G[B]](fga, F.map[G[A => B], G[A] => G[B]](fgab, (gab: G[A => B]) => (ga: G[A]) => G.ap(ga, gab)))
    }

  def traverse[App[+_]] = new Traversable[App] {}

  trait Traversable[App[+_]] {
    def apply[A, B, M[X] <: TraversableOnce[X]](in: M[A])(fn: A => App[B])(implicit App: Applicative[App], cbf: CanBuildFrom[M[A], B, M[B]], executor: ExecutionContext): App[M[B]] = {
      import App._
      in.foldLeft(unit(cbf(in))) { (fr, a) =>
        (fr /^/ fn(a)) (_ += _)
      } |-> (_.result())
    }
  }

}
