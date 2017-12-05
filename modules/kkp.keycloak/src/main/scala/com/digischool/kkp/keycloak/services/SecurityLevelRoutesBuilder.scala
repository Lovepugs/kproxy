package com.digischool.kkp.keycloak.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Directive0
import com.digischool.kkp.core.directives.BasicHttpAuthDirective
import com.digischool.kkp.core.injectable.WithSystem
import com.digischool.kkp.core.models.KModule
import com.digischool.kkp.keycloak.KeycloakAuthenticateModule
import com.digischool.kkp.keycloak.secured.Authentication
import com.kreactive.model.{ApplicationId, Role}
import com.typesafe.config.Config
import org.keycloak.adapters.KeycloakDeployment

trait SecurityLevelRoutesBuilder {
  def logged(roles: List[(ApplicationId, Role)]): Directive0
  def maybeLogged: Directive0
  def appName: ApplicationId
}

object SecurityLevelRoutesBuilder  {
  def apply(config: Config)(implicit system: ActorSystem): SecurityLevelRoutesBuilder = {
    val hc = KModule.getHostConfig(config)
    val kc = KeycloakAuthenticateModule(system).getConfig(config).get
    new SecurityLevelRoutesBuilderImpl(hc.getRoot, kc.deployment)
  }
}

class SecurityLevelRoutesBuilderImpl(rootPath: Path, deployment:KeycloakDeployment)(implicit val system: ActorSystem) extends SecurityLevelRoutesBuilder with Authentication with BasicHttpAuthDirective with WithSystem {
  val handleRejection: Directive0 = handleRejections(ssoRejectionHandler(rootPath,deployment))
  val allow: Directive0 = mayBeAuthenticatedDirective(rootPath, deployment)

  def logged(roles: List[(ApplicationId, Role)]): Directive0 =
    handleRejection & isAuthenticatedDirective(rootPath, deployment, roles)

  val maybeLogged: Directive0 = mayBeAuthenticatedDirective(rootPath, deployment)
  val appName = ApplicationId(deployment.getResourceName)
}