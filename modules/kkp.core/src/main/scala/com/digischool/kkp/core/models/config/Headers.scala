package com.digischool.kkp.core.models.config

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.{Directive0, Directives}
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.digischool.kkp.dsl2.routedsl.generic.rules.MyRule
import com.typesafe.config.Config
import org.parboiled2.ParserInput
import shapeless.LabelledGeneric

case class Header(name: String, value: String)

object Header {
  implicit val gen = LabelledGeneric[Header]
  implicit val parser: CaseClassParser[Header] = CaseClassParser.generic
  implicit val parseable: Parseable[Header] = new Parseable[Header] {
    override val name: String = "Header"
    override def typeParser(input: ParserInput): MyRule[Header] = parser.parensOrNoParens(input)
  }
}

case class Headers(add: List[Header] = Nil, remove: List[String] = Nil) extends CustomDirective {
  def addedHeaders(implicit log: LoggingAdapter) = add.map{
    case Header(name, value) => HttpHeader.parse(name, value)
  }.flatMap{
    case HttpHeader.ParsingResult.Ok(header, _) => List(header)
    case other =>
      log.error(other.errors.map(_.detail).mkString("\n"))
      Nil
  }
  override def apply(system: ActorSystem, v2: Config): Directive0 =
    Directives.mapRequest(_.mapHeaders(_.filterNot(h => remove.contains(h.name)) ++ addedHeaders(Logging(system, getClass))))
}

object Headers {
  implicit val gen = LabelledGeneric[Headers]
  implicit val parser: CaseClassParser[Headers] = CaseClassParser.generic
}
