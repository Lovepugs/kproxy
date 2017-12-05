package com.digischool.kkp.keycloak.models

import com.digischool.kkp.keycloak.models.headers.RolesHeader
import com.kreactive.model.{ApplicationId, Role}
import play.api.libs.json.Json

case class RoleCommand(clientName:ApplicationId, add : List[Role] = Nil, remove:List[Role] = Nil) {
  def isRealmCommand = RolesHeader.isRealm(clientName)
}

case object RoleCommand {
  implicit val format = Json.format[RoleCommand]
}