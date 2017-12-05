package com.digischool.kkp.dsl2.models

import com.digischool.kkp.dsl2.routedsl.{CaseClassParser, DirectiveParser}
import com.digischool.kkp.dsl2.utils.validation.Validation
import org.parboiled2.{ErrorFormatter, ParseError}

import scala.util.Try

trait Directive[+T]

object Directive {
  def apply[T](directives: Directive[T]*) ={
    val multi = MultipartDirective(directives: _*)
    multi.directives match {
      case List(dir) => dir
      case _ => multi
    }
  }

  /**
    * Parses a Directive[T] from a string
    * @param row the string to parse
    * @param filters a map that contains the parsers for [[SimpleDirective[T]], with the key as identifier
    * @tparam T the type for [[SimpleDirective]]
    * @return the parsed directive if the parsing is successful
    *         an `Invalid(err)` if there was an error while parsing in the CaseClassParser
    * @throws ParseError(parse traces) if there was an (unexpected) parsing exception
    */
  def parse[T](row: String, filters: Map[String, CaseClassParser[T]] = Map.empty[String, CaseClassParser[T]]): Try[Validation[Directive[T]]] =
    DirectiveParser(row, filters).OnlyDirective.run().map(Validation.success).recover {
        case e: ParseError => Validation.error(new ErrorFormatter().format(e, row))
      }
}

case class SimpleDirective[T](t: T) extends Directive[T]

case class MultipartDirective[T] private (directives: List[Directive[T]]) extends Directive[T]

object MultipartDirective {
  def apply[T](directives: Directive[T]*): MultipartDirective[T] = MultipartDirective(directives.toList.flatMap {
    case MultipartDirective(subdirectives) => apply(subdirectives: _*).directives
    case other => List(other)
  })
}