package com.digischool.kkp.core.models.config

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive0, Directives}
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.typesafe.config.Config
import shapeless.LabelledGeneric

case class Redirect(url: String, status: StatusCodes.Redirection = StatusCodes.Found) extends CustomDirective {
  override def apply(v1: ActorSystem, v2: Config): Directive0 = Directives.redirect(url, status)
}

object Redirect {
  import StatusCodes._
  val POSSIBLE_CODES: Set[Redirection] =
    Set(MultipleChoices, MovedPermanently, Found, SeeOther, NotModified, UseProxy, TemporaryRedirect, PermanentRedirect)
  implicit val gen = LabelledGeneric[Redirect]
  implicit val statusParseable: Parseable[Redirection] =
    Parseable[Int].collect(n => s"a valid redirect status code, not $n")(Function.unlift(n => POSSIBLE_CODES.find(_.intValue == n)))
  implicit val parser: CaseClassParser[Redirect] = CaseClassParser.generic
}
