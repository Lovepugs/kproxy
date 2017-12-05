package com.digischool.kkp.keycloak.models.config

import akka.http.scaladsl.model.{IllegalUriException, Uri}
import com.digischool.kkp.core.configrepository.KProxyConfig
import com.digischool.kkp.core.injectable.WithNameableLog
import configs.Configs
import org.keycloak.adapters.KeycloakDeployment

import scala.util.Try


case class MigrationTargets(clientids: List[String], wsurls: List[String], forwardurls: List[String], updatepasswordonly: Boolean = false)

case class MigratorConfig(deployment: KeycloakDeployment, targets: List[MigrationTargets]) extends KProxyConfig {
  def log(implicit system: WithNameableLog) = system.logAs(getClass)

  def isValid(implicit system: WithNameableLog): Boolean =
      targets.nonEmpty &&
      targets.forall(validateTarget)

  private def validateTarget(target: MigrationTargets)(implicit system: WithNameableLog): Boolean = {
    target.wsurls.nonEmpty && validateUrls(target.wsurls) &&
      target.clientids.nonEmpty &&
      validateUrls(target.forwardurls)
  }

  private def validateUrls(wsurls: List[String])(implicit system: WithNameableLog): Boolean = wsurls.forall(validateUrl)

  private def validateUrl(wsurl: String)(implicit system: WithNameableLog): Boolean = {
    Try(Uri.apply(wsurl)).recover {
      case e@IllegalUriException(info) =>
        log.error("invalid uri : " + wsurl + " " + info)
        throw e
      case e =>
        log.error("invalid uri : " + wsurl + " " + e.getMessage)
        throw e
    }.isSuccess
  }
}


object MigratorConfig {
  implicit val configs: Configs[MigratorConfig] = Configs.derive[MigratorConfig]
}