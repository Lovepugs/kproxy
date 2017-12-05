package com.digischool.kkp.dsl2.routedsl.generic.rules

import com.digischool.kkp.dsl2.utils.ParserUtils
import org.parboiled2.{ParserInput, Rule1, Rule2, RuleX}

/**
  * A class for subparsers with a specific rule named `myRule`.
  * An attempt at naming rules has been done, so one must implement the abstract method `unnamedRule` instead of `myRule`.
  *
  * @param input The input of the parser
  * @tparam T The expected return type of the rule (cannot be an HList)
  */
abstract class MyRule[+T](val input: ParserInput) extends ParserUtils { self =>
  def myRule: Rule1[T]
}

trait MyTempRule[R <: RuleX] {
  def myTempRule: R
}

/**
  * A class for subparsers which are internaly called using a Rule2[Temp, Kept] but ultimatly return only a Rule1[Kept].
  * Must implement `myTempRule: Rule2[Temp, Kept]`
  * @param input The input of the parser
  * @tparam Temp The type of the first (temporary) value output by the tempRule
  * @tparam Kept The output type of myRule
  */
abstract class MyTwoStageRule[Temp, Kept](input: ParserInput) extends MyRule[Kept](input) with MyTempRule[Rule2[Temp, Kept]] {
  type TempRule = Rule2[Temp, Kept]
  def myRule = rule { myTempRule ~> ((_: Temp, f: Kept) => f) }
}
