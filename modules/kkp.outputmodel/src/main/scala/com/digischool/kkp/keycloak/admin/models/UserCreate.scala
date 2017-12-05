package com.digischool.kkp.keycloak.admin.models

import play.api.libs.json.Json


trait UserCreate

case class  UserPassReset(mail:String){
  //TODO basic regexp check
  def validate = true
}

case class  UserPassCreate(mail:String, password:String, locale:String, username:Option[String], firstName:Option[String], lastName:Option[String]) extends UserCreate

case class  SocialCreate(mail:String, locale:String, username:Option[String], provider: String, providerUserId: String) extends UserCreate


object UserPassCreate{
  implicit val userPassCreateFormat = Json.format[UserPassCreate]
}

object SocialCreate{
  implicit val socialCreateFormat = Json.format[SocialCreate]
}

object UserPassReset{
  implicit val userPassResetFormat = Json.format[UserPassReset]
}

