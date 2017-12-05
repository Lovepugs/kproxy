package com.digischool.kkp.keycloak.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Directive1}
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.digischool.kkp.keycloak.models.headers.{Header, RolesHeader, TokenHeader, UserHeader}
import play.api.libs.json.Reads

trait ExtractTokenInfos {
  type OptHeaders = (Option[UserHeader], Option[RolesHeader], Option[TokenHeader])
  type Headers = (UserHeader, RolesHeader, TokenHeader)

  private def header2directive[H](implicit H: Header[H], R: Reads[H]): Directive1[Option[H]] =
    optionalHeaderValue { h =>
      if (H.headerNames.contains(h.name())) H.decode(h.value()).right.toOption else None
    }

  def maybeExtractInfoFromHeaders: Directive[OptHeaders] =
    header2directive[UserHeader] & header2directive[RolesHeader] & header2directive[TokenHeader]

  def extractInfoFromHeaders: Directive[Headers] = maybeExtractInfoFromHeaders tflatMap {
    case (Some(a), Some(b), Some(c)) => tprovide((a, b, c))
    case _ => complete(StatusCodes.Unauthorized): Directive[Headers]
  }
}

object ExtractTokenInfos extends ExtractTokenInfos
