package com.digischool.kkp.core.directives

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.{Directive0, Directive1}
import com.digischool.kkp.core.utils.UriHelper


trait PortDirective extends UriHelper {

  /**
   * Extracts the hostname part of the Host request header value.
   */
  def extractPort: Directive1[Int] = PortDirective._extractPort

  /**
   * Rejects all requests with a host name different from the given ones.
   */
  def port(ports: Int*): Directive0 = port(ports.contains(_))

  /**
   * Rejects all requests for whose host name the given predicate function returns false.
   */
  def port(predicate: Int ⇒ Boolean): Directive0 = extractPort.require(predicate)

}


object PortDirective extends PortDirective {
  import BasicDirectives._

  private val _extractPort: Directive1[Int] =
    extract{r =>
       r.request.uri.effectivePort
    }

}