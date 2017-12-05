package com.digischool.kkp.keycloak.models.headers

import java.util.UUID

import com.kreactive.model.{ApplicationId, Realm, UserId}
import play.api.libs.json.Json

import scala.io.Source

/**
  * Created by cyrille on 27/05/2016.
  */
class UserHeaderSpec extends HeaderSpec[UserHeader] {
  val example = UserHeader("c.corpet@kreactive.com", "Cyrille", "Corpet", "fr", UserId(UUID.fromString("12345678-1234-1234-1234-123456789abc")), Realm("digiSchool"), ApplicationId("client"))
  val exampleJson = Json.parse(Source.fromURL(getClass.getResource("/headers/userHeaderExample.json")).mkString)

  val examples = Map("basic" -> (example, exampleJson))
  jsonDescriber(examples)

  headerDescriber(examples)
}
