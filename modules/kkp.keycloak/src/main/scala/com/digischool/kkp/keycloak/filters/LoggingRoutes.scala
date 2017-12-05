package com.digischool.kkp.keycloak.filters

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, StandardRoute}
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.keycloak.KeycloakAuthenticateModule
import com.typesafe.config.Config

case object LoggingRoutes extends CustomDirective {
  override def apply(system: ActorSystem, config: Config): Directive0 = {
    val hc = KModule.getHostConfig(config)
    val kc = KeycloakAuthenticateModule(system).getConfig(config).get
    (StandardRoute(KeycloakAuthenticateModule(system).authRoutes(kc.deployment)): Directive0) | pass
  }
}
