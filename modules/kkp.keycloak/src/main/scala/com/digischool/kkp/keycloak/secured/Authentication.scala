package com.digischool.kkp.keycloak.secured

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.SecurityDirectives._
import akka.http.scaladsl.server.directives.Credentials
import com.digischool.kkp.core.directives.{DirectiveHelper, Forwarder}
import com.digischool.kkp.core.injectable._
import com.digischool.kkp.keycloak.directives.{CookieStore, InjectTokenInfos, RolesChecker}
import com.digischool.kkp.keycloak.models.{Channel, TokenRepresentation}
import com.digischool.kkp.keycloak.rejections.{SSOAuthenticationFailedRejection, SsoRejectionHandler}
import com.kreactive.keycloak.authenticator.KeycloakTokenHandler
import com.kreactive.model.{ApplicationId, Role}
import org.keycloak.adapters.KeycloakDeployment


trait Authentication extends
  CookieStore with
  InjectTokenInfos with
  RolesChecker with
  SsoRejectionHandler with
  DirectiveHelper {
  self: WithExecutionContext with
    WithHttpExt with
    WithNameableLog with
    WithMaterializer =>

  def mayBeAuthenticatedDirective(rootPath: Path, deployment: KeycloakDeployment): Directive0 = {
    val isAuthenticated = isAuthenticatedDirective(rootPath, deployment, roles = List())
    val isNotAuthenticated = cancelRejections(classOf[SSOAuthenticationFailedRejection]) & removeInjectedHeaders
    isAuthenticated | isNotAuthenticated
  }

  def extractSsoToken(rootPath: Path, deployment: KeycloakDeployment): Directive1[TokenRepresentation] = {
    //OAuth2 rejection is issued by authHeader
    val authCookieOrFail = authCookie(forceRefresh = false, rootPath)(deployment).flatMap {
      //need to cancel the OAUth2 rejection, since auth is accepted
      case Some(token) => cancelRejections(classOf[AuthenticationFailedRejection]) & provide(token)
      //no need to put another rejection, since it is the same as the one given by authHeader
      case None => reject(): Directive1[TokenRepresentation]
    }
    (authHeader(deployment) | authCookieOrFail) & removeCookiesFromRequest
  }

  def tokenRepresentationAuthenticator(tokenHandler: KeycloakTokenHandler): Authenticator[TokenRepresentation] = {
    case Credentials.Provided(accessTokenStr) =>
      tokenHandler.tokenInfos(accessTokenStr, activeFilter = true)
        .map(infos => TokenRepresentation(infos, accessTokenStr, Channel.ByHeader))
    case _ => None
  }

  def authHeader(deployment: KeycloakDeployment): Directive1[TokenRepresentation] = {
    authenticateOAuth2(deployment.getRealm, tokenRepresentationAuthenticator(KeycloakTokenHandler(deployment)))
  }


  def isAuthenticatedDirective(rootPath: Path, deployment: KeycloakDeployment, roles: List[(ApplicationId, Role)]): Directive0 =
    extractSsoToken(rootPath, deployment) flatMap { tokenRepresentation =>
      checkRoles(roles, tokenRepresentation.accessToken) & injectTokenInfosInHeaders(tokenRepresentation.accessToken, tokenRepresentation.accessTokenStr)
    }
}
