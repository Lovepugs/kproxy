package com.digischool.kkp.keycloak.models.config

import com.digischool.kkp.core.configrepository.KProxyConfig
import com.digischool.kkp.core.injectable.WithNameableLog
import configs.Configs
import org.keycloak.adapters.KeycloakDeployment

case class KeycloakAdminConfig(password: String,
                               deployment: KeycloakDeployment,
                               profileUrl: String,
                               locales: List[String] = Nil) extends KProxyConfig {

  def isValid(implicit system: WithNameableLog): Boolean = password.nonEmpty
}

object KeycloakAdminConfig {

  implicit val configs: Configs[KeycloakAdminConfig] = Configs.derive[KeycloakAdminConfig]
}








