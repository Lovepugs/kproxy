package com.digischool.kkp.keycloak.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.digischool.kkp.keycloak.models.headers.RolesHeader
import com.kreactive.keycloak.authenticator.TokenInfos
import com.kreactive.model.{ApplicationId, Role}

trait RolesChecker {
  def checkRoles(allowedRoles: List[(ApplicationId, Role)], tokenInfos: TokenInfos): Directive0 = {
    val rolesInfo: RolesHeader = RolesHeader(tokenInfos.roles)
    if (allowedRoles.isEmpty || rolesInfo.flattened.exists(allowedRoles.contains)) pass
    else reject(AuthorizationFailedRejection)
  }
}
