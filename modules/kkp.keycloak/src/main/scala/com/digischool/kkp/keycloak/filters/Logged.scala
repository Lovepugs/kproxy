package com.digischool.kkp.keycloak.filters

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive0
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.keycloak.services.SecurityLevelRoutesBuilder
import com.kreactive.model.{ApplicationId, Role}
import com.typesafe.config.Config
import shapeless.LabelledGeneric

case class Logged(roles: List[String] = Nil) extends CustomDirective {
  override def apply(v1: ActorSystem, v2: Config): Directive0 = {
    val routeBuilder = SecurityLevelRoutesBuilder(v2)(v1)
    routeBuilder.logged(Logged.allowedKeyValues(roles, routeBuilder.appName))
  }
}

object Logged {
  def allowedKeyValues(allowedRoles: List[String], appName:ApplicationId):List[(ApplicationId, Role)] = {
    allowedRoles.map(_.split('.')).collect{
      case Array(app, role) => ApplicationId(app) -> Role(role)
      case Array(value)                   => appName -> Role(value)
    }
  }

  implicit val gen = LabelledGeneric[Logged]
  implicit val parser = CaseClassParser[Logged]
}
