package com.digischool.kkp.dsl2.routedsl

import com.digischool.kkp.dsl2.exception.ParsingException
import com.digischool.kkp.dsl2.routedsl.generic.HListParseable
import com.digischool.kkp.dsl2.routedsl.generic.rules.MyRule
import org.parboiled2._
import shapeless.{Default, HList, LabelledGeneric, Witness}

import scala.language.reflectiveCalls
import scala.reflect.{ClassTag, classTag}

trait CaseClassParser[+T] {
  def name: String
  def parse(input: ParserInput): MyRule[T]
  def default: Either[List[String], T]
  def parensOrNoParens(input: ParserInput): MyRule[T] = new MyRule[T](input) {
    def parens: Rule1[T] = rule {
      OptionalSpaces ~ LParen ~ runSubParser(parse(_).myRule).named(s"${name} parameters") ~ OptionalSpaces ~ ((&(Comma) ~ fail(ParsingException.TOO_MANY_PARAMETERS)) | RParen)
    }
    def noParens: Rule1[T] = rule {
      (test(default.isRight) | fail(ParsingException.MISSING_PARAMETER)) ~ push(default.right.get)
    }

    override def myRule: Rule1[T] = rule {
      parens | noParens
    }
  }
}

object CaseClassParser {
  def apply[T](implicit T: CaseClassParser[T]) = T
  def unit[T](T: T, named: String): CaseClassParser[T] = new CaseClassParser[T] {
    override lazy val name: String = named

    override def parse(input: ParserInput): MyRule[T] = new MyRule[T](input) {
      override def myRule: Rule1[T] = rule { push(T) }
    }

    override def parensOrNoParens(input: ParserInput): MyRule[T] = new MyRule[T](input) {
      override def myRule: Rule1[T] = rule { push(T) }
    }

    override def default: Either[List[String], T] = Right(T)
  }
  def unit[T](T: T): CaseClassParser[T] = unit(T, T.toString)

  implicit def generic[T: ClassTag, Options <: HList, Repr <: HList, Def <: HList](implicit gen: LabelledGeneric.Aux[T, Repr], Repr: HListParseable.Aux[Repr, Options], Def: Default.Aux[T, Def], ev: Def <:< Options): CaseClassParser[T] = new CaseClassParser[T] {
    val name: String = classTag[T].runtimeClass.getSimpleName

    override def parse(input: ParserInput): MyRule[T] = new MyRule[T](input) {
      override def myRule: Rule1[T] = rule {
        runSubParser(Repr.recognizeWithDefaults(_, name, Some(Def())).myRule) ~> ((r: HListParseable.Wrapped[Repr]) => gen.from(r.l))
      }
    }
    lazy val default = Repr.resolveDefaults(Def(), None).right.map(gen.from)

  }

  implicit def singleton[T](implicit W: Witness.Aux[T]): CaseClassParser[T] = unit[T](W.value)

  def named[T](implicit c: ClassTag[T], p: CaseClassParser[T]) = c.runtimeClass.getSimpleName -> p
}