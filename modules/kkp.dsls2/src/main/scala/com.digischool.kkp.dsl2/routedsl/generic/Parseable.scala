package com.digischool.kkp.dsl2.routedsl.generic

import com.digischool.kkp.dsl2.exception.ParsingException
import com.digischool.kkp.dsl2.routedsl.generic.rules.MyRule
import org.parboiled2._

import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

/**
  * Created by cyrille on 30/03/2016.
  */
/**
  * A trait that implements a parser for a specific type
  * @tparam T the type we wish to be able to parse
  */
@implicitNotFound("The type ${T} cannot be parsed. Implement an implicit Parseable[${T}]")
trait Parseable[T] { self =>
  def name: String
  def typeParser(input: ParserInput): MyRule[T]
  def map[U](f: T => U, named: Option[String] = None)(implicit U: ClassTag[U] = null): Parseable[U] =
    collect(_ => "")(PartialFunction(f), named)

  def collect[U](expected: T => String)(pf: PartialFunction[T, U], named: Option[String] = None)(implicit U: ClassTag[U] = null): Parseable[U] = new Parseable[U] {
    assert(named.isDefined || (U ne null), "No name found for transformed Parseable")
    val name = named.getOrElse(U.runtimeClass.getSimpleName)

    override def typeParser(input: ParserInput): MyRule[U] = new MyRule[U](input) {

      def handle(t: T): Option[U] = pf.lift(t)
      override def myRule: Rule1[U] = rule {
        runSubParser(self.typeParser(_).myRule).named("mapped parseable") ~> ((t: T) => push(handle(t)) ~ getOrFail(expected(t)))
      }
    }
  }

  def filter(ifError: T => String)(p: T => Boolean, named: Option[String] = None)(implicit T: ClassTag[T] = null) =
    collect[T](ifError)(Function.unlift(t => Some(t).filter(p)), named)
}

object Parseable {
  implicit val stringParseable = new Parseable[String] {
    val name = "String"
    def typeParser(in: ParserInput) = new MyRule[String](in) {
      def myRule: Rule1[String] = rule { QuotedString | Word }
    }
  }

  implicit val intParseable = new Parseable[Int] {
    val name = "Int"
    def typeParser(in: ParserInput) = new MyRule[Int](in) {
      def myRule: Rule1[Int] = rule { IntegralNumber }
    }
  }

  implicit val booleanParseable = new Parseable[Boolean] {
    val name = "Boolean"
    def typeParser(in: ParserInput) = new MyRule[Boolean](in) {
      def trueRule: Rule1[Boolean] = rule {(ignoreCase("true") | ignoreCase("vrai") | ignoreCase('v') | ignoreCase('t')) ~ push(true)}
      def falseRule: Rule1[Boolean] = rule {(ignoreCase("false") | ignoreCase("faux") | ignoreCase('f')) ~ push(false)}
      def myRule: Rule1[Boolean] = rule {trueRule | falseRule}
    }
  }

  implicit def listParseable[T](implicit T: Parseable[T], tag: ClassTag[T]): Parseable[List[T]] = new Parseable[List[T]] {
    val name = s"List[${T.name}]"
    def typeParser(in: ParserInput) = new MyRule[List[T]](in) {
      def myRule: Rule1[List[T]] = rule { LBracket ~ runSubParser(T.typeParser(_).myRule).named(s"$name item").*(Comma) ~ RBracket ~> ((seq: Seq[T]) => seq.toList)}
    }
  }

  implicit def optionParseable[T](implicit T: Parseable[T], tag: ClassTag[T]): Parseable[Option[T]] = new Parseable[Option[T]] {
    val name = s"Option[${T.name}]"
    def typeParser(in: ParserInput) = new MyRule[Option[T]](in) {
      def myRule: Rule1[Option[T]] = rule { runSubParser(T.typeParser(_).myRule).named(s"$name item").? }
    }
  }

  def apply[T](implicit T: Parseable[T]): Parseable[T] = T

}