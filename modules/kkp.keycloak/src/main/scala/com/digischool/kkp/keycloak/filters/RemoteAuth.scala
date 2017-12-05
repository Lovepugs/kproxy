package com.digischool.kkp.keycloak.filters

import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Directive1}
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.digischool.kkp.keycloak.services.UserAdmin
import com.typesafe.config.Config
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import shapeless.LabelledGeneric

import scala.util.{Failure, Success}

case class RemoteAuth(deployment: KeycloakDeployment) extends CustomDirective {
  override def apply(system: ActorSystem, config: Config): Directive0 = {
    getToken(system, deployment) flatMap {token =>
      mapRequest(_.removeHeader("Authorization").addHeader(Authorization(OAuth2BearerToken(token))))
    }
  }

  private def getToken(system: ActorSystem, remoteKeycloak: KeycloakDeployment): Directive1[String] =
    onComplete(UserAdmin(system).getToken(remoteKeycloak, None)) flatMap {
      case Success(token) => provide(token)
      case Failure(err) => complete(StatusCodes.InternalServerError, s"An error occurred while getting token: ${err.getMessage}")
    }
}

object RemoteAuth {

  implicit val deplParser: Parseable[KeycloakDeployment] =
    Parseable[String].map(str => KeycloakDeploymentBuilder.build(new ByteArrayInputStream(str.getBytes)))
  implicit val gen = LabelledGeneric[RemoteAuth]
  implicit val parser = CaseClassParser[RemoteAuth]
}