package com.digischool.kkp.keycloak

import akka.actor.{ActorSystem, ExtendedActorSystem, ExtensionId}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.keycloak.admin.endpoints.UserAdminEndPoint
import com.digischool.kkp.keycloak.models.config.KeycloakAdminConfig
import com.typesafe.config.Config


class KeycloakAdminModule(kernel: KProxyKernel)(implicit system: ActorSystem) extends KModule[KeycloakAdminConfig](kernel) with UserAdminEndPoint {
  val configurationKey = "KeycloakAdminModule"

  private val getOrThrow: (Config) => KeycloakAdminConfig = (config: Config) =>
    getConfig(config).get

    def adminRoles(kac: KeycloakAdminConfig): Route = {
      roles(kac.deployment, kac.password)
    }

    def adminCreateUser(kac: KeycloakAdminConfig): Route = {
      create(kac.deployment, kac.password)
    }

    def adminCreateUserSocial(kac: KeycloakAdminConfig): Route = {
      createSocial(kac.deployment, kac.password)
    }

    def adminResetUserPass(kac: KeycloakAdminConfig): Route = {
      resetPassword(kac.deployment, kac.profileUrl)
    }

    def adminRedirectAccount(kac: KeycloakAdminConfig): Route = {
      redirectAccount(kac.deployment, kac.profileUrl)
    }

    def adminLocales(kac: KeycloakAdminConfig): Route = {
      locale(kac.locales)
    }

    def adminGetUser(kac: KeycloakAdminConfig): Route = {
      user(kac.deployment, kac.password)
    }

    def adminAll(config: Config): Route = {
      val kac = getOrThrow(config)
      adminRedirectAccount(kac) ~ adminRoles(kac) ~ adminCreateUser(kac) ~ adminCreateUserSocial(kac) ~ adminResetUserPass(kac) ~ adminGetUser(kac) ~ adminLocales(kac)
    }

  def route2parser(r: (Config) => Route): CaseClassParser[CustomDirective] =
    CaseClassParser.unit((s: ActorSystem, c: Config) => (StandardRoute(r(c)): Directive0) | pass)

  override def namedFunctions: KModule.PossibleFilters =
    Map(
      "Roles" -> route2parser(adminRoles _ compose getOrThrow),
      "CreateUser" -> route2parser(adminCreateUser _ compose getOrThrow),
      "GetUser" -> route2parser(adminGetUser _ compose getOrThrow),
      "All" -> route2parser(adminAll)
    )

  override def onStart: Unit = ()

  override def onStop(): Unit = ()

}

object KeycloakAdminModule extends ExtensionId[KeycloakAdminModule] {
  override def createExtension(system: ExtendedActorSystem): KeycloakAdminModule = new KeycloakAdminModule(KProxyKernel(system))(system)
}

