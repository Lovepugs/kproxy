package com.digischool.kkp.dsl2.routedsl

import com.digischool.kkp.dsl2.models.{ALL, HTTPMethod, MethodDirective, Methods}
import com.digischool.kkp.dsl2.utils.ParserUtils
import org.parboiled2.Rule1


trait HttpMethodParser extends ParserUtils {

  def testHttpMethods = rule { OptionalSpaces ~ HttpMethods ~ OptionalSpaces ~ EOI }

  def HttpMethods: Rule1[MethodDirective] = rule { All | OrMethods }
  def OrMethods:Rule1[Methods] = rule { HttpMethod.+(OR)  ~> ((s:Seq[HTTPMethod]) => Methods(s.toList))}

  def HttpMethod:Rule1[HTTPMethod] = rule { capture( "GET" | "POST" | "PUT" | "DELETE" | "OPTIONS" | "PATCH" | "HEAD" ) ~> ((s:String) => HTTPMethod.build(s)) }
  def All:Rule1[MethodDirective] = rule { capture("ALL") ~> ((s:String) => ALL ) }

}
