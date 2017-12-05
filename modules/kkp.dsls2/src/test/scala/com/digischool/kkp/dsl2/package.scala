package com.digischool.kkp

import org.parboiled2._
import org.scalatest.TestSuite

import scala.util.{Failure, Success, Try}

/**
  * Created by cyrille on 24/03/2016.
  */
package object dsl2 extends TestSuite {

  lazy val formatter = new ErrorFormatter(showTraces = true)

  @inline
  def ko(str: String) = fail(str)
  @inline
  val ok = succeed

  def parseErrorMessage(parser: Parser, e: ParseError) =
    parser.formatError(e, formatter)

    def parseAndCompare[T](parser: Parser, comput: Try[T], testValue: T, beforeCompare: T => T = identity[T] _) = comput match {
      case Success(t) => beforeCompare(t) === testValue
      case Failure(e: ParseError) => ko(parseErrorMessage(parser, e))
      case Failure(e) => throw e
    }

  def failToParse[T](parser: Parser, comput: Try[T], partialMsg: String = "") = comput match {
      case Failure(e: ParseError) if parseErrorMessage(parser, e).contains(partialMsg) => ok
      case Failure(e: ParseError) => ko(parseErrorMessage(parser, e))
      case Failure(e) => throw e
      case Success(t) => ko(t.toString)
    }

  def failToParse[T](parser: Parser, comput: => Try[T], expected: String, pos: Int) = Try(comput).flatten match {
    case Failure(e: ParseError) if expected == formatter.formatExpectedAsList(e).head && e.position.index == pos => ok
    case Failure(e: ParseError) if expected == formatter.formatExpectedAsList(e).head =>

      ko(new ErrorFormatter().format(e, parser.input) + "\n" +
      s"""got wrong position: ${e.position}
         |        instead of: ${Position(pos, parser.input)}
      """.stripMargin)
    case Failure(e: ParseError) => ko("got unexpected ParseError: " + parseErrorMessage(parser, e))
    case Failure(e) => throw e
    case Success(t) => ko(t.toString)
  }
}
