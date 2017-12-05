package com.digischool.kkp.keycloak.endpoints

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookiePair
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import com.digischool.kkp.core.injectable._
import com.digischool.kkp.core.utils.{HttpUtils, UriHelper}
import com.digischool.kkp.keycloak
import com.digischool.kkp.keycloak.{Action, _}
import com.digischool.kkp.keycloak.directives.CookieStore
import com.digischool.kkp.keycloak.models._
import org.keycloak.OAuth2Constants
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.representations.AccessTokenResponse

import scala.util.{Failure, Success}


trait AuthEndPoints extends
  CookieStore with
  EndPointsAction with
  UriHelper with
  HttpUtils {
  self: WithNameableLog with
    WithExecutionContext with
    WithMaterializer with
    WithHttpExt =>
  
  private lazy val log = logAs("com.digischool.kkp.keycloak.endpoints.AuthEndPoints")
  val REFERER = "Referer"

  def authRoutes(deployment:KeycloakDeployment): Route =
    extractMatchedPath { rootPath =>
      login(deployment, rootPath) ~ logout(deployment, rootPath) ~ authenticate(deployment, rootPath) ~ refresh(deployment, rootPath) ~ register(deployment, rootPath)
    }

  def login(deployment:KeycloakDeployment, rootPath: Path): Route = get{
    path(LOGIN){
      getLogin(deployment, rootPath)
    }
  }

  def logout(deployment:KeycloakDeployment, rootPath: Path): Route = (get | post) {
    path(LOGOUT) {
      authCookie(forceRefresh = false, rootPath)(deployment) { (cookieO: Option[TokenRepresentation]) =>
        logoutCookie(rootPath) {
          getLogoutRedirectUri(rootPath) { redirectUri =>
            drainRequest {
              redirect(deployment.getLogoutUrl.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri).build().toString, StatusCodes.SeeOther)
            }
          }
        }
      }
    }
  }

  private def getLogoutRedirectUri(rootPath: Path): Directive1[String] = parameter(OAuth2Constants.REDIRECT_URI) | (extractRequest map (getHomeUri(_, rootPath).toString))

  def authenticate(deployment:KeycloakDeployment, rootPath: Path): Route =  {
    get {
      path(AUTHENTICATE){
          drainRequest {
            authenticateStep1(deployment, rootPath)
          }
        }
    }
  }

  def refresh(implicit deployment:KeycloakDeployment, rootPath: Path): Route =  {
    get {
      path(REFRESH_TOKEN) {
        parameter(OAuth2Constants.REDIRECT_URI) { redirectUri =>
          authCookie(forceRefresh = true, rootPath)(deployment){ _ =>
            drainRequest {
              redirect(redirectUri, StatusCodes.SeeOther)
            }
          }
        }
      }
    }
  }

  def register(deployment:KeycloakDeployment, rootPath: Path): Route = get{
    path(REGISTER){
      getRegister(deployment, rootPath)
    }
  }

  ///*****************************************************************************************

  private def getXXXUri(xxx: String, request:HttpRequest, rootPath: Path): Uri = {
    val path = if (xxx.nonEmpty) rootPath / xxx else rootPath
    originalSchemeAndPort(request).withPath(path).copy(rawQueryString = None)
  }

  def getLoginUri(request:HttpRequest, rootPath: Path): Uri = {
    getXXXUri(LOGIN, request, rootPath)
  }
  def getLogoutUri(request:HttpRequest, rootPath: Path): Uri = {
    getXXXUri(LOGOUT, request, rootPath)
  }
  def getRegisterUri(request:HttpRequest, rootPath: Path): Uri = {
    getXXXUri(REGISTER, request, rootPath)
  }
  def getRefreshTokenUri(request:HttpRequest, rootPath: Path): Uri = {
    getXXXUri(REFRESH_TOKEN, request, rootPath)
  }
  def getAuthenticateUri(request:HttpRequest, rootPath: Path): Uri = {
    getXXXUri(AUTHENTICATE, request, rootPath)
  }
  def getHomeUri(request:HttpRequest, rootPath: Path): Uri = {
    getXXXUri("", request, rootPath)
  }


  ///*****************************************************************************************


  private def authenticateStep1(implicit deployment:KeycloakDeployment, rootPath: Path): Route = {
    extractRequest{ request =>
      parameters(OAuth2Constants.STATE, OAuth2Constants.CODE, OAuth2Constants.REDIRECT_URI) { (state, code, redirectS) =>
        optionalCookie(HTTP_STATE_CHECKER){
          case Some(HttpCookiePair(_, `state`)) => onComplete(accessCodeToAccessToken(code = code, uri = redirectS, getHomeUri(request, rootPath))){
            case Success(token) => handleToken(token)
            case t @ Failure(e) =>
              log.error(e, "request KO : " + state + " code " +  code)
              redirect(getHomeUri(request, rootPath), StatusCodes.SeeOther)
          }
          case Some(stateCookie) =>
            log.error("invalid state : " + state + " vs " +  stateCookie)
            redirect(getLoginUri(request, rootPath), StatusCodes.SeeOther)
          case None =>
            log.error("No state Cookie Bichette !!! : " + state )
            redirect(getLoginUri(request, rootPath), StatusCodes.SeeOther)
        }
      }
    }
  }

  private def handleToken(accessTokenResponse: AccessTokenResponse)(implicit deployment:KeycloakDeployment, rootPath: Path):Route = {
    val cookieO = CookieData.from(accessTokenResponse)
    cookieO.map{ cookie =>
      loginCookie(cookieData = cookie, path = rootPath){
        parameters(OAuth2Constants.REDIRECT_URI) { uri =>
          redirect(uri, StatusCodes.SeeOther)
        }
      }
    }.getOrElse{
      complete("Invalid accessTokenResponse !!! : " + accessTokenResponse.getToken )
    }
  }


  def getLogin(deployment: KeycloakDeployment, rootPath: Path): Route = {
    getEndPoint(deployment, keycloak.Login, rootPath)
  }

  def getRegister(deployment: KeycloakDeployment, rootPath: Path): Route = {
    getEndPoint(deployment, keycloak.Registration, rootPath)
  }

  def getEndPoint(deployment: KeycloakDeployment, action: Action, rootPath: Path): Route = {
    val state = UUID.randomUUID().toString
    setState(state, rootPath){
      extractRequest{ request =>
        val uri = originalSchemeAndPort(request)

        val localeO = uri.authority.host.toString.split(':').headOption.flatMap(_.split('.').lastOption).map{
          case "uk" => "en"
          case any => any
        }

        val unencodedUri = unencodedRedirectUrl(uri, action)

        val endPoint = deployment.getEndPointFromAction(action)
          .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
          .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName)
          .queryParam(OAuth2Constants.STATE, state)
          .queryParam("kc_locale", localeO.getOrElse(""))
          .queryParam(OAuth2Constants.REDIRECT_URI, safeRedirectUri(getHomeUri(request, rootPath), unencodedUri))
          .build()
          .toString

        drainRequest & redirect(Uri(endPoint), StatusCodes.SeeOther)
      }
    }
  }

  /**
    * redirect to home to prevent infinites redirections
    */
  private def unencodedRedirectUrl(uri:Uri, action: Action) = (action match {
    case keycloak.Login | keycloak.Registration if uri.path.startsWith(Path / LOGIN) | uri.path.startsWith(Path / REGISTER) =>
      uri.withPath(Path.SingleSlash)
    case _ => uri
  }).toString
}

class StaticAuthEndPoints(implicit val system: ActorSystem) extends WithSystem with AuthEndPoints
