package com.digischool.kkp.keycloak.filters

import akka.http.scaladsl.model.{MediaRanges, StatusCodes}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Accept, Location, `WWW-Authenticate`}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.digischool.kkp.keycloak.RoutingSpec
import com.digischool.kkp.keycloak.services.SecurityLevelRoutesBuilderImpl
import com.kreactive.keycloak.authenticator.KeycloakTokenHandler
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.OptionValues._

class LoggedSpec extends RoutingSpec with  WordSpecLike with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  val kcDeployment = KeycloakTokenHandler(
    """
      |{
      |  "realm": "digiSchool",
      |  "auth-server-url": "https://testaccount.digischool.io/auth",
      |  "ssl-required": "external",
      |  "resource": "api",
      |  "public-client": true,
      |  "use-resource-role-mappings": true
      |}
    """.stripMargin).get.keycloakDeployment

  val logged = new SecurityLevelRoutesBuilderImpl(Path.Empty, kcDeployment).logged(Nil)
  
  "a logged route" should {
    "redirect to keycloak if no token or cookie is present, and txt/html is accepted" in {
      Get("https://userservice.do/me").withHeaders(Accept(MediaRanges.`*/*`)) ~> logged { completeOk } ~> check {
        status shouldEqual StatusCodes.SeeOther
        val locationO: Option[Location] = header[Location]
        locationO.value.uri.authority.host.address() shouldBe "testaccount.digischool.io"
      }
    }
    "return a 401 if no token or cookie is present, and txt/html is not accepted" in {
      Get("https://userservice.do/me").withHeaders(Accept(MediaRanges.`application/*`)) ~> logged { completeOk } ~> check {
        status shouldEqual StatusCodes.Unauthorized
        val challenge = header[`WWW-Authenticate`].value.challenges.headOption.value
        challenge.realm shouldEqual "digiSchool"
        challenge.scheme shouldEqual "Bearer"
      }
    }
  }
}