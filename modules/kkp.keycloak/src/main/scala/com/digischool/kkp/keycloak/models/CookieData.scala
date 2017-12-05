package com.digischool.kkp.keycloak.models


import com.kreactive.keycloak.authenticator.{KeycloakTokenHandler, TokenInfos}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.jose.jws.JWSInput
import org.keycloak.representations.{AccessTokenResponse, RefreshToken}

import scala.util.Try


case class CookieData(tokenInfos: TokenInfos, accessTokenStr:String, idTokenStr: Option[String], refreshTokenStr: String)  {

  def isActive: Boolean = tokenInfos.active

  def refreshToken: Option[RefreshToken] = CookieData.getRefreshToken(refreshTokenStr)

  def toTokenRepresentation = TokenRepresentation(tokenInfos, accessTokenStr, Channel.ByCookie)

}

object CookieData {

  def from(accessTokenResponse: AccessTokenResponse)(implicit deployment: KeycloakDeployment): Option[CookieData] = {
    for{
      tokenInfos <- KeycloakTokenHandler(deployment).tokenInfos(accessTokenResponse.getToken)
    } yield {
      CookieData(tokenInfos,
        accessTokenResponse.getToken,
        Option(accessTokenResponse.getIdToken),
        accessTokenResponse.getRefreshToken
      )
    }
  }

  def getRefreshToken(refreshStr:String):Option[RefreshToken]={
    val cleanup = refreshStr.trim()
    if(cleanup.isEmpty){
      None
    } else{
      Try{
        val input:JWSInput  = new JWSInput(cleanup)
        input.readJsonContent(classOf[RefreshToken])
      }.toOption
    }
  }

}