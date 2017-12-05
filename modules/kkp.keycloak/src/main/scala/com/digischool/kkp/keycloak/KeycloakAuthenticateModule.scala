package com.digischool.kkp.keycloak

import akka.actor.{ActorSystem, ExtendedActorSystem, ExtensionId}
import akka.http.scaladsl.server._
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.models.KModule
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.keycloak.endpoints.{AdminCallbackEndPoints, AuthEndPoints}
import com.digischool.kkp.keycloak.filters._
import com.digischool.kkp.keycloak.models.config.KeycloakConfig
import com.digischool.kkp.keycloak.services.SecurityLevelRoutesBuilder
import com.typesafe.config.Config

import scala.util.Try


class KeycloakAuthenticateModule(kernel: KProxyKernel)(implicit system: ActorSystem) extends KModule[KeycloakConfig](kernel) with AuthEndPoints with AdminCallbackEndPoints {
  val configurationKey = "keycloakModule"

  override def namedFunctions: KModule.PossibleFilters = Map(
    CaseClassParser.named[Logged],
    CaseClassParser.named[BasicAuth],
    CaseClassParser.named[RemoteAuth],
    CaseClassParser.named[UpdateScopes],
    "Allow" -> CaseClassParser.unit((system: ActorSystem, conf: Config) => Try(SecurityLevelRoutesBuilder(conf)(system).maybeLogged).getOrElse(Directives.pass)),
    "LoggingRoutes" -> CaseClassParser.unit(LoggingRoutes),
    "AdminRoutes" -> CaseClassParser.unit((_: ActorSystem, conf: Config) => (StandardRoute(adminRoutes(getConfig(conf).get.deployment)): Directive0) | Directives.pass)
  )
  override def onStart(): Unit = ()

  override def onStop(): Unit = ()

}

object KeycloakAuthenticateModule extends ExtensionId[KeycloakAuthenticateModule]{
  override def createExtension(system: ExtendedActorSystem): KeycloakAuthenticateModule =
    new KeycloakAuthenticateModule(KProxyKernel(system))(system)
}