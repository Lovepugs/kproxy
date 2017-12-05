package com.digischool.kkp.core.directives

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import akka.http.scaladsl.server.{Directive0, Directives}
import com.digischool.kkp.dsl2.models._

object ParsedMethodDirective {
  def directive(m: MethodDirective) = m match {
    case Methods(meths) if meths.nonEmpty => methods(meths)
    case _ => Directives.pass
  }

  private def methods(methods: List[HTTPMethod]): Directive0 =
    methods.map((HTTPMethod2HttpMethod _) andThen (Directives.method _)).reduce(_ | _)

  private def HTTPMethod2HttpMethod(method: HTTPMethod): HttpMethod = {
    method match {
      case GET => HttpMethods.GET
      case POST => HttpMethods.POST
      case PUT => HttpMethods.PUT
      case DELETE => HttpMethods.DELETE
      case OPTIONS => HttpMethods.OPTIONS
      case PATCH => HttpMethods.PATCH
      case HEAD => HttpMethods.HEAD
    }
  }
}
