package com.digischool.kkp.keycloak.admin.endpoints


import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import com.digischool.kkp.core.injectable.{WithExecutionContext, WithHttpExt, WithMaterializer, WithNameableLog}
import com.digischool.kkp.core.utils.{HttpUtils, UriHelper}
import org.keycloak.adapters.KeycloakDeployment

import scala.concurrent.Future


trait ResetPasswordFlow extends HttpUtils with UriHelper {
  self: WithMaterializer with WithExecutionContext with WithNameableLog with WithHttpExt =>
  private lazy val resetPasswordLog = logAs("com.digischool.kkp.keycloak.admin.endpoints.ResetPasswordFlow")

  def resetPassUrl(deployment: KeycloakDeployment, profileUrl: String): Future[(Uri, Cookie)] = {
    val url = s"${deployment.getAuthServerBaseUrl}/realms/${deployment.getRealm}/login-actions/reset-credentials?client_id=account"
    for {
      (location, cookie) <- step1(deployment, url)
    } yield (location, cookie)

  }

  // Get form url and cookieValue
  def step1(deployment: KeycloakDeployment, resetUrl: String): Future[(Uri, Cookie)] = {

    val fResponse: Future[(HttpResponse, String)] = keepRequestAndUnmarshal[String] {
      HttpRequest(
        uri = resetUrl,
        method = HttpMethods.GET)
    }

    (for {
      (response, htmlString) <- fResponse
      url = Uri(s"${deployment.getAuthServerBaseUrl}/realms/${deployment.getRealm}/login-actions/reset-credentials?code=${extractResetUrl(deployment, htmlString)}")
      cookie = response.header[`Set-Cookie`].get.cookie
    } yield {
      resetPasswordLog.info("Step1 URL " + url)
      resetPasswordLog.info("Step1 cookie " + cookie.pair())
      (url, Cookie(cookie.pair))
    }).recover {
      case e => resetPasswordLog.error("STEP 2 " + e)
        throw e
    }
  }

  private def extractResetUrl(deployment: KeycloakDeployment, htmlString: String) = {
    val pattern = s"/auth/realms/${deployment.getRealm}/login-actions/reset-credentials"
    htmlString.split(pattern)(1).split('"')(0).stripPrefix("?code=")
  }


}
