package com.digischool.kkp.keycloak.directives

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Cookie, HttpCookie}
import akka.http.scaladsl.model.{DateTime, HttpHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.digischool.kkp.core.injectable.{WithExecutionContext, WithHttpExt, WithMaterializer}
import com.digischool.kkp.keycloak.models._
import com.digischool.kkp.keycloak.services.TokenHandler
import com.kreactive.keycloak.authenticator.KeycloakTokenHandler
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.representations.RefreshToken

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try


trait CookieStore extends TokenHandler  { self: WithExecutionContext with WithHttpExt with WithMaterializer =>

  lazy val cookieNames = Set(KPROXY_ACCESS, KPROXY_ID, KPROXY_REFRESH)
  lazy val findKproxyAccess: (HttpHeader) => Option[String] = findCookie(KPROXY_ACCESS)
  lazy val findKproxyRefresh: (HttpHeader) => Option[String] = findCookie(KPROXY_REFRESH)
  lazy val findKproxyId: (HttpHeader) => Option[String] = findCookie(KPROXY_ID)

  def setPathCookie(name: String, value: String, path: Path = Path.Empty, expires: Option[DateTime] = None): Directive0 =
    setCookie(HttpCookie(name, value, path = Some(path.toString).filterNot(_.isEmpty).orElse(Some("/")), expires = expires))

  def deletePathCookie(name: String, path: Path): Directive0 = deleteCookie(name, path = Some(path.toString).filterNot(_.isEmpty).getOrElse("/"))

  def setState(state:String, path: Path) : Directive0 =
    setPathCookie(HTTP_STATE_CHECKER, state, path)

  def deleteState(path:Path) : Directive0 = deleteCookie(HTTP_STATE_CHECKER, path = path.toString)

  def logoutCookie(path:Path) : Directive0 =
    deleteCookie(KPROXY_ACCESS, path = path.toString) &
    deleteCookie(KPROXY_REFRESH, path = path.toString) &
    deleteCookie(KPROXY_ID, path = path.toString)

  def loginCookie(cookieData: CookieData, path:Path) : Directive0 = {
    val refreshO: Option[RefreshToken] = cookieData.refreshToken
    val exp = refreshO.map{refresh =>
      val timestamp = refresh.getExpiration.seconds
      DateTime(timestamp.toMillis)
    }
    setPathCookie(KPROXY_ACCESS, cookieData.accessTokenStr, path, expires = exp) &
    setPathCookie(KPROXY_REFRESH, cookieData.refreshTokenStr, path, expires = exp) &
      cookieData.idTokenStr.fold(pass)(setPathCookie(KPROXY_ID, _, path, expires = exp)) & deleteState(path)
  }

  val removeCookiesFromRequest =
    mapRequest { req =>
      val keptCookiesO = Some(req.cookies.filterNot(h => cookieNames.contains(h.name))).filterNot(_.isEmpty)
      val noCookies = req.removeHeader(Cookie.name)
        keptCookiesO.fold(noCookies)(kc => noCookies.addHeader(Cookie(kc)))
    }

  def authCookie(forceRefresh:Boolean, path: Path)(implicit deployment: KeycloakDeployment): Directive1[Option[TokenRepresentation]] = {
    if (deployment.isBearerOnly){
      provide(Option.empty[TokenRepresentation]) & logoutCookie(path)
    }else{
      authWithNewCookie(forceRefresh).flatMap { oon: Option[(CookieData, CookieData)] =>
        oon match {
          case Some((oldCookie, newCookie)) if oldCookie != newCookie =>
              loginCookie(newCookie, path) & provide(Option(newCookie.toTokenRepresentation)) //use Option.apply to help compiler for implicit conversions
          case Some((_, newCookie)) =>
            provide(Some(newCookie.toTokenRepresentation))
          case _ => provide(Option.empty[TokenRepresentation]) & logoutCookie(path)
        }
      }
    }
  }

  ////////////////////////////////////PRIVATE////////////////////////////////////////////////


  private def authWithNewCookie(forceRefresh:Boolean)(implicit deployment: KeycloakDeployment): Directive1[Option[(CookieData, CookieData)]] = {
    for{
      cookieO <- getAuthCookie
      newCookieO <- authRenewedCookie(cookieO, forceRefresh)
    } yield{
      for{
        oldCookie <- cookieO
        newCookie <- newCookieO
      } yield { (oldCookie, newCookie)}
    }
  }

  private def authRenewedCookie(oldCookieO:Option[CookieData], forceRefresh:Boolean)(implicit deployment: KeycloakDeployment): Directive1[Option[CookieData]] = {
    onComplete(checkValidCookie(oldCookieO, forceRefresh)).map{ (newCookieDataT: Try[CookieData]) =>
      val newCookieO = newCookieDataT.toOption
      newCookieO
    }
  }

  private def findCookie(name: String): HttpHeader ⇒ Option[String] = {
    case Cookie(cookies) => cookies.find(_.name == name).map(_.value)
    case _               => None
  }


  private def getAuthCookie(implicit deployment: KeycloakDeployment) : Directive1[Option[CookieData]]= {
    // CAUTION : mayBe Expired token
    for{
      oA <- optionalHeaderValue(findKproxyAccess)
      oR <- optionalHeaderValue(findKproxyRefresh)
      idTokenStr <- optionalHeaderValue(findKproxyId)
    } yield {
      for {
        accessTokenStr <- oA
        refreshTokenStr <- oR
        tokenInfo <- KeycloakTokenHandler(deployment).tokenInfos(accessTokenStr)
      } yield {
        CookieData(tokenInfo,
          accessTokenStr,
          idTokenStr,
          refreshTokenStr
        )
      }
    }
  }

  private def checkValidCookie(cookieDataO:Option[CookieData], forceRefresh:Boolean)(implicit deployment: KeycloakDeployment): Future[CookieData] = cookieDataO match {
    case Some(cookieData) if !forceRefresh && cookieData.isActive =>
      Future.successful(cookieData)
    case Some(cookieData) =>
      invokeRefresh(cookieData.refreshTokenStr).flatMap { accessTokenResponse =>
        CookieData.from(accessTokenResponse).map { newCookieData =>
          Future.successful(newCookieData)
        }.getOrElse(Future.failed(new Exception("refresh unable to convert to CookieData")))
      }
    case None => Future.failed(new Exception("No cookie provided"))
  }
 }
