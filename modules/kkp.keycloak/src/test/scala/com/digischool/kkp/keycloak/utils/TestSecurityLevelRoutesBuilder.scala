package com.digischool.kkp.keycloak.utils

import akka.http.scaladsl.server.{Directive0, Directives}
import akka.http.scaladsl.server.directives.RouteDirectives
import com.digischool.kkp.keycloak.services.SecurityLevelRoutesBuilder
import com.kreactive.model.{ApplicationId, Role}


case object TestSecurityLevelRoutesBuilder extends SecurityLevelRoutesBuilder with RouteDirectives {

  def logged(roles:List[(ApplicationId, Role)]): Directive0 = complete("LOGGED")

  override def appName: ApplicationId = ApplicationId("application")

  override def maybeLogged: Directive0 = Directives.pass
}
