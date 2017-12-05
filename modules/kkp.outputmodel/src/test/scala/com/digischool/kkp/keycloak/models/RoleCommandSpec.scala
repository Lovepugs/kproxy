package com.digischool.kkp.keycloak.models

import com.kreactive.model.{ApplicationId, Role}
import play.api.libs.json.Json

import scala.io.Source

/**
  * Created by cyrille on 27/05/2016.
  */
class RoleCommandSpec extends JsonSpec[RoleCommand] {

  val companion = RoleCommand
  val client = ApplicationId("client")

  val roleCommandExample = RoleCommand(client, List(Role("admin"), Role("toto")), List(Role("tata"), Role("titi")))
  val roleCommandExampleJson = Json.parse(Source.fromURL(getClass.getResource("/roleCommand/roleCommandExample.json")).mkString)

  val emptyRoleCommandExample = RoleCommand(client)
  val emptyRoleCommandExampleJson = Json.parse(Source.fromURL(getClass.getResource("/roleCommand/emptyRoleCommandExample.json")).mkString)

  "A RoleCommand" should {

    "be able to check that it is a realm command" in {
      RoleCommand(ApplicationId("realm")).isRealmCommand shouldBe true
    }

    "be able to check that it is a REALM command" in {
      RoleCommand(ApplicationId("REALM")).isRealmCommand shouldBe true
    }

    "be able to check that it is not a realm command" in {
      RoleCommand(client).isRealmCommand shouldBe false
    }
  }

  jsonDescriber(Map(
    "basic" -> (roleCommandExample, roleCommandExampleJson),
    "empty" -> (emptyRoleCommandExample, emptyRoleCommandExampleJson)
  ))

}
