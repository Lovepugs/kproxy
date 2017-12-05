package com.digischool.kkp.commonfilters.filters

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.DebuggingDirectives
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.typesafe.config.Config
import shapeless.LabelledGeneric

case class UriLogger(logLevel: LogLevel = Logging.InfoLevel, name: String = "SimpleLoggingFilter") extends CustomDirective {

  override def apply(system: ActorSystem, v2: Config): Directive0 = {
    DebuggingDirectives.logResult(name, logLevel)
  }
}

object UriLogger {
  implicit val logLevelParseable =
    Parseable[String].collect(l => s"not recognized log level: $l")(Function.unlift(Logging.levelFor))
  implicit val gen = LabelledGeneric[UriLogger]
  implicit val parser = CaseClassParser[UriLogger]
}
