package com.digischool.kkp.core.directives

import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server._


trait OriginalPortDirective {
  /**
    * Extracts the original port part of the request provided by the "X-Forwarded-Port" header value
    * or the one of the request
    *
    */
  def extractOriginalPort: Directive1[Int] = OriginalPortDirective._extractOriginalPort

  /**
    * Rejects all requests with an original port value different from the given ones.
    */
  def originalPort(ports: Int*): Directive0 = originalPort(ports.contains(_))

  /**
    * Rejects all requests for whose original part value the given predicate function returns false.

    */
  def originalPort(predicate: Int ⇒ Boolean): Directive0 = extractOriginalPort.require(predicate)

}

object OriginalPortDirective extends OriginalPortDirective with PortDirective{
  import BasicDirectives._

  private val _extractOriginalPort: Directive1[Int] =
    extract(rc => withPort(rc.request))

}