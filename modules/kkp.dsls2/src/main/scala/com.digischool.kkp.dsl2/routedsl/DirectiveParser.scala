package com.digischool.kkp.dsl2.routedsl

import com.digischool.kkp.dsl2.exception.ParsingException
import com.digischool.kkp.dsl2.models.{Directive => mDirective, _}
import org.parboiled2._

import scala.collection.immutable.SortedMap

/**
  * Created by cyrille on 23/03/2016.
  */
case class DirectiveParser[T](input: ParserInput, available: Map[String, CaseClassParser[T]] = Map.empty[String, CaseClassParser[T]]) extends HttpMethodParser with PathParser {
  val sortedRules = SortedMap[String, CaseClassParser[T]]()(Ordering.String.reverse) ++ available

  def OnlyDirective = rule { OptionalSpaces ~ Directive ~ EOI}

  def Directive = rule { InlineDirective | (fail(ParsingException.EMPTY) ~ push(mDirective())) }

  def MultiLineDirective: Rule1[mDirective[T]] = rule {
    LBrace ~ InlineDirective.+(Spaces) ~ RBrace ~> ((s: Seq[mDirective[T]]) => mDirective(s: _*))
  }
  def InlineDirective: Rule1[mDirective[T]] = rule {
    (HttpMethods | Path | NamedDirective | MultiLineDirective).+(InlineSpaces) ~> ((s: Seq[mDirective[T]]) => mDirective(s: _*))
  }

  def TestSeveral = rule {
    OptionalSpaces ~ NamedDirective.+(OptionalSpaces) ~> (_.toList) ~ OptionalSpaces ~ EOI
  }

  def TestNamed = rule {
    OptionalSpaces ~ NamedDirective ~ OptionalSpaces ~ EOI
  }

  def NamedDirective: Rule1[SimpleDirective[T]] = rule {
    valueMap(sortedRules) ~> ((f: CaseClassParser[T]) => runSubParser(f.parensOrNoParens(_).myRule).named(f.name + " parameter list")) ~> ((t: T) => SimpleDirective[T](t))
  }

  override val explicitPath: Boolean = false
}
