package com.digischool.kkp.core.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.http.scaladsl.model.{HttpRequest, Uri}

import scala.util.Try


trait UriHelper {
  //private lazy val log = Logging(system,getClass)
  def withPort(request: HttpRequest): Int =
    request.headers.find(_.name() == "X-Forwarded-Port").map(_.value.toInt).getOrElse(request.uri.authority.port)

  def withScheme(request: HttpRequest): String =
    request.headers.find(_.name() == "X-Forwarded-Proto").map(_.value()).getOrElse(request.uri.scheme)

  def withHost(request: HttpRequest): String =
    request.headers.find(_.name() == "X-Forwarded-For").map(_.value()).getOrElse(request.uri.authority.host.address())

  def withSchemeHostAndPort(uri: Uri, scheme: String, host: String, port: Int): Uri = {
    val toNormalize = uri.withScheme(scheme).withHost(host).withPort(port)
    toNormalize.withAuthority(toNormalize.authority.normalizedFor(scheme))
  }

  def originalSchemeAndPort(request: HttpRequest): Uri =
    withSchemeHostAndPort(request.uri, withScheme(request), request.uri.authority.host.address(), withPort(request))

  def originalSchemeHostAndPort(request: HttpRequest): Uri =
    withSchemeHostAndPort(request.uri, withScheme(request), withHost(request), withPort(request))

  def safeEncode(str: String): String =
    Try(URLEncoder.encode(str, StandardCharsets.UTF_8.displayName())).getOrElse(str)
}
