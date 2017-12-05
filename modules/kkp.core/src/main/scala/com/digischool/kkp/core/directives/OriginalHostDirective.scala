package com.digischool.kkp.core.directives

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.digischool.kkp.core.utils.UriHelper

import scala.util.matching.Regex


trait OriginalHostDirective extends UriHelper {
  /**
    * Extracts the original hostname part of the provided by the "X-Forwarded-For" header value or the one of the Host request header
    *
    */
  def extractOriginalHost: Directive1[String] = OriginalHostDirective._extractOriginalHost

  /**
    * Rejects all requests with an original host name different from the given ones.
    *
    * @group host
    */
  def originalHost(hostNames: String*): Directive0 = originalHost(hostNames.contains(_))

  /**
    * Rejects all requests for whose original host name the given predicate function returns false.
    *
    * @group host
    */
  def originalHost(predicate: String ⇒ Boolean): Directive0 = extractOriginalHost.require(predicate)


  /**
    * Rejects all requests with an original host name that doesn't have a prefix matching the given regular expression.
    * For all matching requests the prefix string matching the regex is extracted and passed to the inner route.
    * If the regex contains a capturing group only the string matched by this group is extracted.
    * If the regex contains more than one capturing group an IllegalArgumentException is thrown.
    *
    * @group host
    */
  def originalHost(regex: Regex): Directive1[String] = {
    import OriginalHostDirective.EnhancedRegex
    def forFunc(regexMatch: String ⇒ Option[String]): Directive1[String] =
      extractOriginalHost.flatMap(regexMatch(_).map(provide).getOrElse(reject))

    regex.groupCount match {
      case 0 ⇒ forFunc(regex.findPrefixOf(_))
      case 1 ⇒ forFunc(regex.findPrefixMatchOf(_).map(_.group(1)))
      case _ ⇒ throw new IllegalArgumentException("Path regex '" + regex.pattern.pattern +
        "' must not contain more than one capturing group")
    }
  }

}

object OriginalHostDirective extends OriginalHostDirective {
  import BasicDirectives._

  private val _extractOriginalHost: Directive1[String] =
    extract(rc => withHost(rc.request))

  implicit class EnhancedRegex(val regex: Regex) extends AnyVal {
    def groupCount = regex.pattern.matcher("").groupCount()
  }

}