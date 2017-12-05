package com.digischool.kkp.core.directives

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.digischool.kkp.core.utils.UriHelper

import scala.util.matching.Regex


trait OriginalSchemeDirective extends UriHelper {
  import BasicDirectives._

  /**
    * Extracts the "Original" Uri scheme from the request handling "X-Forwarded-Proto".
    */
  def extractOriginalScheme: Directive1[String] = OriginalSchemeDirective._extractOriginalScheme

  /**
    * Rejects all requests whose Uri scheme does not match the given one.
    */
  def originalScheme(name: String): Directive0 =
    extractOriginalScheme.require(_ == name, SchemeRejection(name)) & cancelRejections(classOf[SchemeRejection])
}

object OriginalSchemeDirective extends OriginalSchemeDirective {
  import BasicDirectives._

  private val _extractOriginalScheme: Directive1[String] =
    extract(rc => withScheme(rc.request))

}
