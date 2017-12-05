package com.digischool.kkp.keycloak.models.headers

import com.kreactive.model.{ApplicationId, Realm, UserId}
import play.api.libs.json.{Json, OFormat}

/**
  * Created by cyrille on 12/05/2016.
  */
case class UserHeader(
                       email: String,
                       firstName: String,
                       lastName: String,
                       locale: String,
                       id: UserId,
                       realm: Realm,
                       issuedFor: ApplicationId
                     )

case object UserHeader {
  implicit val format: OFormat[UserHeader] =
    Json.format[UserHeader]
  implicit val header: Header[UserHeader] =
    Header.asJson[UserHeader]("KPROXY-USER")
}