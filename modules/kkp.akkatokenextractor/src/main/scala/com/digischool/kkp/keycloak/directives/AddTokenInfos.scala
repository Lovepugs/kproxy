package com.digischool.kkp.keycloak.directives

import akka.http.scaladsl.model.HttpHeader
import com.digischool.kkp.keycloak.models.headers._


trait AddTokenInfos {

  def makeTokenHeaders(user: UserHeader, roles: RolesHeader, token: TokenHeader): Seq[HttpHeader] = {
    val userHeaders = Header[UserHeader].setHeaderAs(user)
    val roleHeaders = Header[RolesHeader].setHeaderAs(roles)
    val tokenHeaders = Header[TokenHeader].setHeaderAs(token)

    (userHeaders ++ roleHeaders ++ tokenHeaders).toSeq.map{
      case (name, value) => HttpHeader.parse(name, value)
    }.collect{
      case HttpHeader.ParsingResult.Ok(header, _) => header
    }
  }

}

object AddTokenInfos extends AddTokenInfos