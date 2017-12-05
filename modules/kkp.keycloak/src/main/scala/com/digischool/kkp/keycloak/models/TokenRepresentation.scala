package com.digischool.kkp.keycloak.models

import com.kreactive.keycloak.authenticator.TokenInfos

sealed trait Channel

object Channel {
  case object ByHeader extends Channel
  case object ByCookie extends Channel
}


case class TokenRepresentation(accessToken: TokenInfos, accessTokenStr:String, channel: Channel)
