package com.digischool.kkp.dsl2

import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import shapeless.LabelledGeneric

trait Testable

case class Logged(roles: List[String] = Nil) extends Testable

object Logged {
  implicit val gen = LabelledGeneric[Logged]
  implicit val parser: CaseClassParser[Logged] = CaseClassParser.generic
}

case class Backend(host: String, port: Int = 80, path: String = "/") extends Testable

object Backend {
  implicit val gen = LabelledGeneric[Backend]
  implicit val parser: CaseClassParser[Backend] = CaseClassParser.generic
}

