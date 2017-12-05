package com.digischool.kkp.commonfilters.filters

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.TimeoutDirectives
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.typesafe.config.Config
import shapeless.LabelledGeneric

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

case class RequestTimeout(duration: FiniteDuration) extends CustomDirective {
  override def apply(system: ActorSystem, config: Config): Directive0 =
    TimeoutDirectives.withRequestTimeout(duration)
}

object RequestTimeout {
  implicit val duration: Parseable[FiniteDuration] = Parseable[String].
    collect("a valid duration, not: " + _)(Function.unlift(d => Try(Duration(d)).toOption)).
    collect("a finite duration, not: " + _){
      case f: FiniteDuration => f
    }
  implicit val gen = LabelledGeneric[RequestTimeout]
  implicit val parser: CaseClassParser[RequestTimeout] = CaseClassParser.generic
}
