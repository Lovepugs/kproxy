package com.digischool.kkp.core.utils.cors

import akka.http.scaladsl.model.HttpMethods.OPTIONS
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpHeader, HttpMethod, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.mapResponse
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.{BasicDirectives, RespondWithDirectives, RouteDirectives}
import akka.http.scaladsl.server.{Directive0, Directive1, RejectionHandler}

import scala.collection.immutable.Seq

/**
  * Created by cyrille on 09/12/2016.
  */
trait CorsUtils {

  import BasicDirectives._
  import CorsUtils._
  import RespondWithDirectives._
  import RouteDirectives._

  /**
    * Wraps its inner route with support for the CORS mechanism, enabling cross origin requests.
    *
    * In particular the recommendation written by the W3C in https://www.w3.org/TR/cors/ is
    * implemented by this directive.
    *
    * @param settings the settings used by the CORS filter
    */
  def cors(settings: CorsSettings = CorsSettings.defaultSettings, overrideExisting: Boolean = false): Directive0 = corsDecorate(settings, overrideExisting).map(_ ⇒ ())

  /**
    * Wraps its inner route with support for the CORS mechanism, enabling cross origin requests.
    * Provides to the inner route an object that indicates if the current request is a valid CORS
    * actual request or is outside the scope of the specification.
    *
    * In particular the recommendation written by the W3C in https://www.w3.org/TR/cors/ is
    * implemented by this directive.
    *
    * @param settings the settings used by the CORS filter
    */
  def corsDecorate(settings: CorsSettings = CorsSettings.defaultSettings, overrideExisting: Boolean = false): Directive1[CorsDecorate] = {

    import settings._

    def accessControlExposeHeaders: Option[`Access-Control-Expose-Headers`] = {
      if (exposedHeaders.nonEmpty) Some(`Access-Control-Expose-Headers`(exposedHeaders))
      else None
    }

    def accessControlAllowCredentials: Option[`Access-Control-Allow-Credentials`] = {
      if (allowCredentials) Some(`Access-Control-Allow-Credentials`(true))
      else None
    }

    def accessControlMaxAge: Option[`Access-Control-Max-Age`] = {
      maxAge.map(`Access-Control-Max-Age`.apply)
    }

    def accessControlAllowHeaders(requestHeaders: Seq[String]): Option[`Access-Control-Allow-Headers`] = allowedHeaders match {
      case HttpHeaderRange.Default(headers) ⇒ Some(`Access-Control-Allow-Headers`(headers))
      case HttpHeaderRange.* if requestHeaders.nonEmpty ⇒ Some(`Access-Control-Allow-Headers`(requestHeaders))
      case _ ⇒ None
    }

    def accessControlAllowMethods = {
      `Access-Control-Allow-Methods`(allowedMethods)
    }

    def accessControlAllowOrigin(origins: Seq[HttpOrigin]): `Access-Control-Allow-Origin` = {
      if (allowedOrigins == HttpOriginRange.* && !allowCredentials) {
        `Access-Control-Allow-Origin`.*
      } else {
        `Access-Control-Allow-Origin`.forRange(HttpOriginRange.Default(origins))
      }
    }

    /** Return an invalid origin, or `None` if they are all valid. */
    def validateOrigin(origins: Seq[HttpOrigin]): Option[HttpOrigin] =
      origins.find(!allowedOrigins.matches(_))

    /** Return the method if invalid, `None` otherwise. */
    def validateMethod(method: HttpMethod): Option[HttpMethod] =
      Some(method).filterNot(allowedMethods.contains)

    /** Return the list of invalid headers, or `None` if they are all valid. */
    def validateHeaders(headers: Seq[String]): Option[Seq[String]] =
      Some(headers.filterNot(allowedHeaders.matches)).filter(_.nonEmpty)

    extractRequest.flatMap { request ⇒
      import request._

      (method, header[Origin].map(_.origins), header[`Access-Control-Request-Method`].map(_.method)) match {
        case (OPTIONS, Some(origins), Some(requestMethod)) if origins.size == 1 ⇒
          // Case 1: pre-flight CORS request

          val headers = header[`Access-Control-Request-Headers`].map(_.headers).getOrElse(Seq.empty)

          def completePreflight = {
            val responseHeaders = Seq(accessControlAllowOrigin(origins), accessControlAllowMethods) ++
              accessControlAllowHeaders(headers) ++ accessControlMaxAge ++ accessControlAllowCredentials
            complete(HttpResponse(StatusCodes.OK, responseHeaders))
          }

          (validateOrigin(origins), validateMethod(requestMethod), validateHeaders(headers)) match {
            case (None, None, None) ⇒
              completePreflight
            case (invalidOrigin, invalidMethod, invalidHeaders) ⇒
              reject(CorsRejection(invalidOrigin, invalidMethod, invalidHeaders))
          }

        case (_, Some(origins), None) if origins.nonEmpty ⇒
          // Case 2: actual CORS request

          val decorate: CorsDecorate = CorsDecorate.CorsRequest(origins)
          val responseHeaders: Seq[HttpHeader] = Seq(accessControlAllowOrigin(origins)) ++
            accessControlExposeHeaders ++ accessControlAllowCredentials

          validateOrigin(origins) match {
            case None ⇒
              val response = if (overrideExisting)
                respondWithHeaders(responseHeaders) & removeResponseHeaders(responseHeaders)
              else
                respondWithDefaultHeaders(responseHeaders)
              response & provide(decorate)
            case invalidOrigin ⇒
              reject(CorsRejection(invalidOrigin, None, None))
          }

        case _ if allowGenericHttpRequests ⇒
          // Case 3a: not a valid CORS request, but allowed

          provide(CorsDecorate.NotCorsRequest)

        case _ ⇒
          // Case 3b: not a valid CORS request, forbidden

          reject(CorsRejection(None, None, None))
      }
    }
  }

}

object CorsUtils extends CorsUtils {

  def corsRejectionHandler: RejectionHandler = RejectionHandler.newBuilder().handle {
    case CorsRejection(None, None, None) ⇒
      complete((StatusCodes.BadRequest, "The CORS request is malformed"))
    case CorsRejection(origin, method, headers) ⇒
      val messages = Seq(
        origin.map("invalid origin '" + _ + "'"),
        method.map("invalid method '" + _.value + "'"),
        headers.map("invalid headers '" + _.mkString(",") + "'")
      ).flatten
      complete((StatusCodes.BadRequest, "CORS: " + messages.mkString(", ")))
  }.result()

  sealed abstract class CorsDecorate {
    def isCorsRequest: Boolean
  }

  object CorsDecorate {

    case class CorsRequest(origins: Seq[HttpOrigin]) extends CorsDecorate {
      def isCorsRequest = true
    }

    case object NotCorsRequest extends CorsDecorate {
      def isCorsRequest = false
    }

  }

  def removeResponseHeaders(headers: Seq[HttpHeader]) = {
    val headerNames = headers.map(_.name()).toSet
    mapResponse(_.mapHeaders(_.filterNot(h => headerNames.contains(h.name()))))
  }


}

