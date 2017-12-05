package com.digischool.kkp.keycloak.models

import com.kreactive.model.Role
import play.api.libs.json.Json

case class RoleRepresentation(id:String, name: Role, scopeParamRequired:Boolean, composite:Boolean) {

}

object RoleRepresentation{
  implicit val roleRepresentationFormat = Json.format[RoleRepresentation]
}

