package com.digischool.kkp.core.services

import com.digischool.kkp.core.directives.{ParsedMethodDirective, ParsedPathDirective}
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.dsl2.models.path.Path
import com.digischool.kkp.dsl2.models.{Directive, MethodDirective, MultipartDirective, SimpleDirective}
import play.api.libs.functional.Monoid

trait ParsedDirectiveInterpreter[T, U] {
  def interpretMethod(m: MethodDirective): U
  def interpretPath(p: Path): U
  def interpretCustom: PartialFunction[T, U]

  def fold(d: Directive[T])(implicit T: Monoid[U]): U = d match {
    case meth: MethodDirective => interpretMethod(meth)
    case path: Path => interpretPath(path)
    case SimpleDirective(c) if interpretCustom.isDefinedAt(c) => interpretCustom(c)
    // throws error if run against an unexpected custom directive
    case MultipartDirective(dirs) => dirs.map(fold).fold(T.identity)(T.append)
  }
}

trait DirectiveInterpreter extends ParsedDirectiveInterpreter[CustomDirective, CustomDirective] {
  def interpretMethod(m: MethodDirective): CustomDirective = CustomDirective((_, _) => ParsedMethodDirective.directive(m), m.toString)
  def interpretPath(p: Path): CustomDirective = CustomDirective((_, _) => ParsedPathDirective.directive(p), p.toString)
}

object DefaultDirectiveInterpreter extends DirectiveInterpreter {
  override def interpretCustom: PartialFunction[CustomDirective, CustomDirective] = PartialFunction(identity[CustomDirective])
}

object StringDirectiveInterpreter extends ParsedDirectiveInterpreter[CustomDirective, String] {
  override def interpretMethod(m: MethodDirective): String = m.toString
  override def interpretPath(p: Path): String = p.toString
  override def interpretCustom: PartialFunction[CustomDirective, String] = PartialFunction(_.toString)

  implicit val stringMonoid = new Monoid[String] {
    override def append(a1: String, a2: String): String = s"$a1\n$a2"

    override def identity: String = ""
  }
}