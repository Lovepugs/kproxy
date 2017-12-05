package com.digischool.kkp.keycloak.endpoints

import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.model.{HttpEntity, ResponseEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.digischool.kkp.core.injectable.WithNameableLog
import org.keycloak.adapters.KeycloakDeployment



trait AdminCallbackEndPoints { self: WithNameableLog =>
  private lazy val log = logAs("com.digischool.kkp.keycloak.endpoints.AdminCallbackEndPoints")

  def adminRoutes(deployment:KeycloakDeployment): Route = callback(deployment:KeycloakDeployment)

  def logEntity(entity: ResponseEntity): ResponseEntity = entity match {
    case e @ HttpEntity.Strict(contentType, data) =>
      log.debug("LOG ENTITY : " + data.utf8String)
      e
    case _ => throw new IllegalStateException("LOG ENTITY : Unexpected entity type")
  }

  private def callback(deployment:KeycloakDeployment): Route = post{
    pathPrefix("callback") {
      pathPrefix("k_logout") {
        extractRequest { req =>
          logEntity(req.entity)
          complete("muchas gracias Callback ") //FIXME
        }
      }
    }
  }


}