package com.digischool.kkp.keycloak.models.headers

import com.kreactive.model.{ApplicationId, Role}
import com.kreactive.util.MapUtils
import play.api.libs.json.Json

/**
  * Created by cyrille on 12/05/2016.
  */
case class RolesHeader(roles: Map[ApplicationId, List[Role]]) {
  def isMailValid = getRealmRoles.contains(RolesHeader.MAIL_VALIDE)
  def isMailBounce = getRealmRoles.contains(RolesHeader.MAIL_BOUNCE)
  def flattened: Seq[(ApplicationId, Role)] = roles.toSeq.flatMap{
    case (app, rs) => rs.map(app -> _)
  }
  def getRealmRoles = roles.getOrElse(RolesHeader.REALM, Nil)
  def getAppRoles(app: ApplicationId) = roles.getOrElse(app, Nil)
}

case object RolesHeader extends RolesConstants with MapUtils {
  def isRealm(app: ApplicationId) = app.value.toLowerCase == REALM.value
  implicit val keyFormat = mapFormat[ApplicationId, List[Role]]
  implicit val format = Json.format[RolesHeader]

  implicit val header: Header[RolesHeader] =
    Header.asJson[RolesHeader]("KPROXY-ROLES")

}

trait RolesConstants {
  val REALM = ApplicationId("realm")
  val MAIL_VALIDE = Role("mail_valide")
  val MAIL_BOUNCE = Role("mail_bounce")
}

