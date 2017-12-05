package com.digischool.kkp.keycloak.admin.endpoints

import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.server.Directives._
import com.kreactive.model.UserId

trait AdminEndPointsAction {
  val UserIdSegment: PathMatcher1[UserId] = JavaUUID.map(UserId(_))
  val ROLE: PathMatcher1[UserId] = ("admin" / "user" / UserIdSegment / "roles")
  val GET_USER: PathMatcher1[UserId] = "admin" / "user" / UserIdSegment
  val CREATE_USER_PASS = "admin" / "user" / "userpass"
  val CREATE_SOCIAL = "admin" / "user" /  "provider"
  val RESET_USER_PASS = "admin" / "user" / "resetpassword"
  val VALID_LOCALES = "admin" / "locales"
}