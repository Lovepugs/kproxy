package com.digischool.kkp.dsl2.models

sealed trait MethodDirective extends Directive[Nothing]
case object ALL extends MethodDirective
case class Methods(verbs: List[HTTPMethod]) extends MethodDirective

object Methods {
  def apply(methods: HTTPMethod*): Methods = Methods(methods.toList)
}

sealed trait HTTPMethod

case object GET extends HTTPMethod
case object POST  extends HTTPMethod
case object PUT extends HTTPMethod
case object DELETE  extends HTTPMethod
case object OPTIONS  extends HTTPMethod
case object PATCH extends HTTPMethod
case object HEAD  extends HTTPMethod


object HTTPMethod {
  def build(str: String):HTTPMethod = {
    str match {
      case  "GET" => GET
      case  "POST" => POST
      case  "PUT" => PUT
      case  "DELETE" => DELETE
      case  "OPTIONS" => OPTIONS
      case  "PATCH" => PATCH
      case  "HEAD" => HEAD
    }
  }
}