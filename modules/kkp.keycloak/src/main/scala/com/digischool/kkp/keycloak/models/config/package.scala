package com.digischool.kkp.keycloak.models

import java.io.ByteArrayInputStream

import com.typesafe.config.{Config, ConfigRenderOptions}
import configs.Configs
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}

package object config {
  implicit val deploymentConfigs: Configs[KeycloakDeployment] =
    Configs[String].orElse(Configs[Config].map(_.root().render(ConfigRenderOptions.concise()))).map(_.getBytes).map(new ByteArrayInputStream(_)).map(KeycloakDeploymentBuilder.build)
}
