package com.digischool.kkp.keycloak.models

import java.util.UUID

import com.kreactive.capsule.ValueClass
import com.kreactive.model.ApplicationId
import play.api.libs.json.Json


case class ClientId(id: UUID) extends AnyVal {
  override def toString = id.toString
}

object ClientId extends ValueClass[UUID, ClientId]{
  override def construct: (UUID) => ClientId = apply
  override def deconstruct: (ClientId) => UUID = _.id
}

case class ClientRepresentationLight(id:ClientId, clientId: ApplicationId) {
}

object ClientRepresentationLight{
  implicit val clientRepresentationLightFormat = Json.format[ClientRepresentationLight]
}