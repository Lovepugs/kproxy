package com.digischool.kkp.keycloak.filters

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive0
import com.digischool.kkp.core.directives.BasicHttpAuthDirective
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.keycloak.KeycloakAuthenticateModule
import com.typesafe.config.Config
import shapeless.LabelledGeneric

case class BasicAuth(login: String, password: String) extends CustomDirective {
  override def apply(system: ActorSystem, config: Config): Directive0 = {
    val realm = KeycloakAuthenticateModule(system).getConfig(config).get.deployment.getRealm
    BasicHttpAuthDirective.basicHttpAuth0(login, password, Some(realm))
  }
}

object BasicAuth {
  implicit val gen = LabelledGeneric[BasicAuth]
  implicit val parser: CaseClassParser[BasicAuth] = CaseClassParser.generic
}