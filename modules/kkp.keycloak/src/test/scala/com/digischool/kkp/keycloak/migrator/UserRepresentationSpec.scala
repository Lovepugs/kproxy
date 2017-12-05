package com.digischool.kkp.keycloak.migrator

import com.digischool.kkp.keycloak.models.UserRepresentation
import com.kreactive.model.{ApplicationId, Role}
import org.scalatest.{TestSuite, WordSpecLike}
import play.api.libs.json.Json


object UserRepresentationSpec extends TestSuite with WordSpecLike {

  val myUser = UserRepresentation(None, "f@k.com", "fred", "masion", "f@k.com")
  val toto = ApplicationId("toto")
  val admin = Role("admin")
  val superAdmin = Role("superadmin")

  "The user" should {
    "serialize in" in {
      Json.toJson(myUser).toString() === """{"username":"f@k.com","firstName":"fred","lastName":"masion","email":"f@k.com","attributes":{},"enabled":true,"emailVerified":false,"requiredActions":[]}"""
    }
  }

  "The user with locale" should {
    val updatedUser = myUser.withLocale("fr")
    "serialize in" in {
      Json.toJson(updatedUser).toString() === """{"username":"f@k.com","firstName":"fred","lastName":"masion","email":"f@k.com","attributes":{"locale":["fr"]},"enabled":true,"emailVerified":false,"requiredActions":[]}"""
    }
  }

  "The user with realm role" should {
    val updatedUser = myUser.withRealmRole(admin)
    "serialize in" in {
      Json.toJson(updatedUser).toString() === """{"username":"f@k.com","firstName":"fred","lastName":"masion","email":"f@k.com","attributes":{},"enabled":true,"emailVerified":false,"requiredActions":[],"realmRoles":["admin"]}"""
    }
  }

  "The user with 2 realm roles" should {
    val updatedUser = myUser.withRealmRole(admin).withRealmRole(superAdmin)
    "serialize in" in {
      Json.toJson(updatedUser).toString() === """{"username":"f@k.com","firstName":"fred","lastName":"masion","email":"f@k.com","attributes":{},"enabled":true,"emailVerified":false,"requiredActions":[],"realmRoles":["admin","superadmin"]}"""
    }
  }

  "The user with client role" should {
    val updatedUser = myUser.withClientRole(toto,admin)
    "serialize in" in {
      Json.toJson(updatedUser).toString() === """{"username":"f@k.com","firstName":"fred","lastName":"masion","email":"f@k.com","attributes":{},"enabled":true,"emailVerified":false,"requiredActions":[],"clientRoles":{"toto":["admin"]}}"""
    }
  }

  "The user with 2 client roles" should {
    val updatedUser = myUser.withClientRole(toto, admin).withClientRole(toto, superAdmin)
    "serialize in" in {
      Json.toJson(updatedUser).toString() === """{"username":"f@k.com","firstName":"fred","lastName":"masion","email":"f@k.com","attributes":{},"enabled":true,"emailVerified":false,"requiredActions":[],"clientRoles":{"toto":["admin","superadmin"]}}"""
    }
  }

}
