package com.digischool.kkp

import java.util

import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.authentication.ClientIdAndSecretCredentialsProvider
import org.keycloak.common.util.KeycloakUriBuilder


package object keycloak {

  sealed trait Action
  case object Login extends Action
  case object Registration extends Action

  implicit class keycloakDeploymentOps(deployment:KeycloakDeployment) extends scala.AnyRef  {

    def clientsecret: String = {
      val credentials: Option[util.Map[String, AnyRef]] = Option(deployment.getResourceCredentials).filterNot(_.isEmpty)
      val secretO: Option[String] = credentials.map{_.get(ClientIdAndSecretCredentialsProvider.PROVIDER_ID).asInstanceOf[String]}
      secretO.getOrElse(throw new IllegalArgumentException("Missing secret in keycloak installation json"))
    }

    def registerUrl: KeycloakUriBuilder = {
      deployment.getAuthUrl.clone()
        .replacePath(s"/auth/realms/${deployment.getRealm}/protocol/openid-connect/registrations")
    }

    def loginUrl: KeycloakUriBuilder = deployment.getAuthUrl.clone()

    def getEndPointFromAction(action: Action) = {
      action match {
        case Login => loginUrl
        case Registration => registerUrl
      }
    }
  }

}
