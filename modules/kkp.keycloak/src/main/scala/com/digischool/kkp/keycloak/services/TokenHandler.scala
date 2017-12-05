package com.digischool.kkp.keycloak.services

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.digischool.kkp.core.injectable.{WithExecutionContext, WithHttpExt, WithMaterializer}
import com.digischool.kkp.core.utils.{HttpUtils, UriHelper}
import com.digischool.kkp.keycloak.endpoints.EndPointsAction
import org.keycloak.OAuth2Constants
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils
import org.keycloak.representations.AccessTokenResponse
import org.keycloak.util.JsonSerialization

import scala.collection.JavaConverters._
import scala.collection.immutable.{Seq => ISeq}
import scala.collection.mutable
import scala.concurrent.Future


trait TokenHandler extends EndPointsAction with UriHelper {
  self: WithExecutionContext with WithHttpExt with WithMaterializer =>

  def accessCodeToAccessToken(code: String, uri: String, rootUri: Uri)(implicit deployment: KeycloakDeployment): Future[AccessTokenResponse] = {
    val formData = scala.collection.mutable.Map(
      OAuth2Constants.GRANT_TYPE -> OAuth2Constants.AUTHORIZATION_CODE,
      OAuth2Constants.CODE -> code,
      OAuth2Constants.REDIRECT_URI -> safeRedirectUri(rootUri, uri)
    )
    performRequest(formData)
  }

  def invokeRefresh(refreshTokenStr: String)(implicit deployment: KeycloakDeployment): Future[AccessTokenResponse] = {
    val formData = scala.collection.mutable.Map(
      OAuth2Constants.GRANT_TYPE -> OAuth2Constants.REFRESH_TOKEN,
      OAuth2Constants.REFRESH_TOKEN -> refreshTokenStr
    )
    performRequest(formData)
  }

  def grantToken(user: String, password: String)(implicit deployment: KeycloakDeployment): Future[AccessTokenResponse] = {
    val formData = scala.collection.mutable.Map(
      OAuth2Constants.GRANT_TYPE -> OAuth2Constants.PASSWORD,
      OAuth2Constants.PASSWORD -> password,
      "username" -> user
    )
    performRequest(formData)
  }

  def clientAccess(clientSecret: String)(implicit deployment: KeycloakDeployment): Future[AccessTokenResponse] = {
    val formData = scala.collection.mutable.Map(
      OAuth2Constants.GRANT_TYPE -> OAuth2Constants.CLIENT_CREDENTIALS,
      OAuth2Constants.CLIENT_ID -> deployment.getResourceName,
      OAuth2Constants.CLIENT_SECRET -> clientSecret
    )

    performRequest(formData)
  }

  private def performRequest(formData: scala.collection.mutable.Map[String, String], moreHeaders: ISeq[HttpHeader] = ISeq.empty)(implicit deployment: KeycloakDeployment): Future[AccessTokenResponse] = {
    val kTokenUrl = deployment.getTokenUrl
    val headers = scala.collection.mutable.Map[String, String]()
    ClientCredentialsProviderUtils.setClientCredentials(deployment, headers.asJava, formData.asJava)
    val fEntity = Marshal(FormData(Map() ++ formData)).to[RequestEntity]
    val headersSeq: mutable.Iterable[RawHeader] = headers.map { case (k, v) => RawHeader(k, v) }

    for {
      entity <- fEntity
      request = HttpRequest(uri = kTokenUrl, method = HttpMethods.POST, entity = entity, headers = moreHeaders ++ headersSeq)
      response <- HttpUtils.requestAndUnmarshalOK[String](request)
    } yield JsonSerialization.readValue(response, classOf[AccessTokenResponse]) //this may fail, but is safe in Future.map
  }

  def safeRedirectUri(rootUri: Uri, unencodedUri: String) = {
    //keep rootUrl path if HostConfig has nonEmpty root
    val redirectUri = rootUri.withPath(rootUri.path / AUTHENTICATE).
      //use rawQueryString because Uri.Query does not properly urlEncode uris
      withRawQueryString(OAuth2Constants.REDIRECT_URI + "=" + safeEncode(unencodedUri))
    redirectUri.toString
  }
}
