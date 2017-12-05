package com.digischool.kkp.keycloak

import akka.actor.{ActorSystem, ExtendedActorSystem, ExtensionId}
import akka.http.scaladsl.server._
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.models.KModule
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.keycloak.migrator.directives.MigratorDirectives
import com.digischool.kkp.keycloak.models.config.MigratorConfig
import com.typesafe.config.Config


class KeycloakMigratorModule(kernel: KProxyKernel)(implicit system: ActorSystem) extends KModule[MigratorConfig](kernel) with MigratorDirectives {
  val configurationKey = "keycloakMigrator"

  def mgd(system: ActorSystem, config: Config) = {
    val hc = KModule.getHostConfig(config)
    val mc = getConfig(config).get
    loginAction(mc, hc)
  }

  override def namedFunctions: KModule.PossibleFilters =
    Map(
      "Migrator" -> CaseClassParser.unit(mgd _)
    )

  override def onStart: Unit = ()

  override def onStop(): Unit = ()

}

object KeycloakMigratorModule extends ExtensionId[KeycloakMigratorModule] {
  override def createExtension(system: ExtendedActorSystem): KeycloakMigratorModule =
    new KeycloakMigratorModule(KProxyKernel(system))(system)
}

