package com.digischool.kkp.keycloak.models.config

import com.digischool.kkp.core.configrepository.KProxyConfig
import com.digischool.kkp.core.injectable.WithNameableLog
import configs.Configs
import org.keycloak.adapters.KeycloakDeployment

case class KeycloakConfig(deployment: KeycloakDeployment) extends KProxyConfig {

  def isValid(implicit system: WithNameableLog): Boolean = true
}


object KeycloakConfig {

  implicit val configs: Configs[KeycloakConfig] = Configs.derive[KeycloakConfig]

}