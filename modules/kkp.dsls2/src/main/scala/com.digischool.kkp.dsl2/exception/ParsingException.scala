package com.digischool.kkp.dsl2.exception

import org.parboiled2.Position

/**
  * Created by cyrille on 22/04/2016.
  */
class ParsingException(val expected: String, val position: Position)

object ParsingException {
  val EMPTY = "non empty"
  val TOO_MANY_PARAMETERS = "no more parameters"
  val UNNAMED_BEFORE_NAMED = "named parameters after other named parameters"
  val TWICE_DEFINED_PARAMETER = "not already defined parameter"
  val NO_COMMA_AT_START = "no comma at start"
  val INVALID_PARAMETER = "valid parameter"
  val MISSING_PARAMETER = "more parameters"
  def expectType(`type`: String) = "parameter of type " + `type`
}