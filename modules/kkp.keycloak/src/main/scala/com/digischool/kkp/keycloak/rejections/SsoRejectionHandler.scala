package com.digischool.kkp.keycloak.rejections

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Accept, HttpChallenges}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{BasicDirectives, ExecutionDirectives, RouteDirectives}
import com.digischool.kkp.core.injectable._
import com.digischool.kkp.keycloak.endpoints.AuthEndPoints
import org.keycloak.adapters.KeycloakDeployment

case class SSOAuthenticationFailedRejection(underlying: AuthenticationFailedRejection) extends Rejection

trait SsoRejectionHandler extends
  AuthEndPoints with
  BasicDirectives with
  RouteDirectives with
  ExecutionDirectives {
  self: WithNameableLog with
    WithExecutionContext with
    WithMaterializer with
    WithHttpExt =>

  def oauthRejectionToSso: PartialFunction[Rejection, Route] = {
    case auth: AuthenticationFailedRejection if auth.challenge == HttpChallenges.oAuth2(auth.challenge.realm) => reject(SSOAuthenticationFailedRejection(auth))
  }

  def ssoRejectionHandler(rootPath: Path, deployment: KeycloakDeployment) = {
    RejectionHandler.newBuilder().handle(oauthRejectionToSso)
      .handle {
        case SSOAuthenticationFailedRejection(auth) =>
          extractAcceptHtml { acceptHtml =>
            if (acceptHtml)
              getLogin(deployment, rootPath)
            else
              handleRejections(RejectionHandler.default)(reject(auth))
          }
      }.result().seal
  }

  def extractAcceptHtml = {
    extract(_.request.header[Accept].filterNot(_.mediaRanges.isEmpty).
      exists(//if no or empty Accept, assume it's curl or equivalent -> noHtml
        _.mediaRanges.exists { mr =>
          mr.qValue() > 0.00000000001 && mr.matches(MediaTypes.`text/html`) //if text/html is acceptable, then return true
        }
      )
    )
  }
}
