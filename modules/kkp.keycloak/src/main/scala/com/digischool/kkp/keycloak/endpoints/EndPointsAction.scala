package com.digischool.kkp.keycloak.endpoints

trait EndPointsAction {
  val LOGIN = "login"
  val LOGOUT = "logout"
  val REGISTER = "register"
  val AUTHENTICATE = "authenticate"
  val REFRESH_TOKEN = "refreshtoken"
}