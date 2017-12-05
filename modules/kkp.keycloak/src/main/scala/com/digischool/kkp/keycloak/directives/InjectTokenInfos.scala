package com.digischool.kkp
package keycloak.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.digischool.kkp.keycloak.models.headers._
import com.kreactive.keycloak.authenticator.TokenInfos


trait InjectTokenInfos extends AddTokenInfos {
  val headersNames = Header[UserHeader].headerNames ++ Header[RolesHeader].headerNames ++ Header[TokenHeader].headerNames

  val removeInjectedHeaders =
    mapRequest(_.mapHeaders(_.filterNot(h => headersNames.contains(h.name))))

  def injectTokenInfosInHeaders(infos: TokenInfos, accessStr:String):Directive0 =
    removeInjectedHeaders &
      addHeaders(
        makeTokenHeaders(
          tokenInfosToUserHeader(infos),
          RolesHeader(infos.roles),
          TokenHeader(accessStr)
        )
      )

  private def addHeaders(headers: Seq[HttpHeader]) =
    mapRequest(_.mapHeaders(_ ++ headers))

  private def tokenInfosToUserHeader(infos: TokenInfos): UserHeader = UserHeader(
    infos.optional.email.getOrElse(""),
    infos.optional.firstname.getOrElse(""),
    infos.optional.lastname.getOrElse(""),
    infos.optional.locale.getOrElse("fr"),
    infos.userId,
    infos.realm,
    infos.app
  )
}
