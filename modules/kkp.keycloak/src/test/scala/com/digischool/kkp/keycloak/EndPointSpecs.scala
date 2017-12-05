package com.digischool.kkp.keycloak

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.digischool.kkp.core.models.config.HostConfiguration
import com.digischool.kkp.keycloak.endpoints.{AuthEndPoints, StaticAuthEndPoints}
import akka.http.scaladsl.server.Directives._
import com.digischool.kkp.keycloak.utils.{TestConfigurationRepository, TestSecurityLevelRoutesBuilder}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class EndPointSpecs extends RoutingSpec with  WordSpecLike with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  val testSecurityRouteBuilder = TestSecurityLevelRoutesBuilder

  val toto: HostConfiguration = TestConfigurationRepository.configs(0)
  val titi: HostConfiguration = TestConfigurationRepository.configs(1)
  val AuthEndPoints = new StaticAuthEndPoints()(system)

  val testLoginToto = extractRequest{request =>
    redirect(AuthEndPoints.getLoginUri(request, toto.getRoot), StatusCodes.SeeOther)
  }
  val testLoginTiti = extractRequest{request =>
    redirect(AuthEndPoints.getLoginUri(request, titi.getRoot), StatusCodes.SeeOther)
  }



  val testLogoutTiti = extractRequest{request =>
    redirect(AuthEndPoints.getLogoutUri(request, titi.getRoot), StatusCodes.SeeOther)
  }
  val testAuthenticateTiti = extractRequest{request =>
    redirect(AuthEndPoints.getAuthenticateUri(request, titi.getRoot), StatusCodes.SeeOther)
  }
  val testRefreshTokenTiti = extractRequest{request =>
    redirect(AuthEndPoints.getRefreshTokenUri(request, titi.getRoot), StatusCodes.SeeOther)
  }



// with HTTPS
  """REDIRECT WITH SCHEME AND PREFIX  """ should {
    """Get path("https://www.example.com/" to /login without prefix)""" in {
      Get("https://www.example.com/") ~> testLoginToto ~> check {
        status shouldEqual StatusCodes.SeeOther
        val locationO: Option[Location] = header[Location]
        locationO shouldEqual(Some(Location("https://www.example.com/login")))
      }
    }

    """Get path("https://www.example.com/" to /login WITH prefix /titi)""" in {
      Get("https://www.example.com/") ~> testLoginTiti ~> check {
        status shouldEqual StatusCodes.SeeOther
        val locationO: Option[Location] = header[Location]
        locationO shouldEqual(Some(Location("https://www.example.com/titi/login")))
      }
    }


// other paths

    """Get path("https://www.example.com/" to /logout WITH prefix /titi)""" in {
      Get("https://www.example.com/") ~> testLogoutTiti ~> check {
        status shouldEqual StatusCodes.SeeOther
        val locationO: Option[Location] = header[Location]
        locationO shouldEqual(Some(Location("https://www.example.com/titi/logout")))
      }
    }

    """Get path("https://www.example.com/" to /authenticate WITH prefix /titi)""" in {
      Get("https://www.example.com/") ~> testAuthenticateTiti ~> check {
        status shouldEqual StatusCodes.SeeOther
        val locationO: Option[Location] = header[Location]
        locationO shouldEqual(Some(Location("https://www.example.com/titi/authenticate")))
      }
    }

    """Get path("https://www.example.com/" to /refreshtoken WITH prefix /titi)""" in {
      Get("https://www.example.com/") ~> testRefreshTokenTiti ~> check {
        status shouldEqual StatusCodes.SeeOther
        val locationO: Option[Location] = header[Location]
        locationO shouldEqual(Some(Location("https://www.example.com/titi/refreshtoken")))
      }
    }

  }


  // with HTTP
  """Get path("http://www.example.com/" to /login without prefix)""" in {
    Get("http://www.example.com/") ~> testLoginToto ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("http://www.example.com/login")))
    }
  }

  """Get path("http://www.example.com/" to /login WITH prefix /titi)""" in {
    Get("http://www.example.com/") ~> testLoginTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("http://www.example.com/titi/login")))
    }
  }

  """Get path("http://www.example.com/" to /logout WITH prefix /titi)""" in {
    Get("http://www.example.com/") ~> testLogoutTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("http://www.example.com/titi/logout")))
    }
  }

  """Get path("http://www.example.com/" to /authenticate WITH prefix /titi)""" in {
    Get("http://www.example.com/") ~> testAuthenticateTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("http://www.example.com/titi/authenticate")))
    }
  }

  """Get path("http://www.example.com/" to /refreshtoken WITH prefix /titi)""" in {
    Get("http://www.example.com/") ~> testRefreshTokenTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("http://www.example.com/titi/refreshtoken")))
    }
  }

   //with HTTP and X-Forwarded-Proto=HTTPS
  """Get path("http://www.example.com/" to /login without prefix with X-Forwarded-Proto=HTTPS)""" in {
    Get("http://www.example.com/").withHeaders(RawHeader("X-Forwarded-Proto","HTTPS")) ~> testLoginToto ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("https://www.example.com/login")))
    }
  }

  """Get path("http://www.example.com/" to /login WITH prefix /titi with X-Forwarded-Proto=HTTPS)""" in {
    Get("http://www.example.com/").withHeaders(RawHeader("X-Forwarded-Proto","HTTPS")) ~> testLoginTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("https://www.example.com/titi/login")))
    }
  }

  """Get path("http://www.example.com/" to /logout WITH prefix /titi with X-Forwarded-Proto=HTTPS)""" in {
    Get("http://www.example.com/").withHeaders(RawHeader("X-Forwarded-Proto","HTTPS")) ~> testLogoutTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("https://www.example.com/titi/logout")))
    }
  }

  """Get path("http://www.example.com/" to /authenticate WITH prefix /titi with X-Forwarded-Proto=HTTPS)""" in {
    Get("http://www.example.com/").withHeaders(RawHeader("X-Forwarded-Proto","HTTPS")) ~> testAuthenticateTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("https://www.example.com/titi/authenticate")))
    }
  }

  """Get path("http://www.example.com/" to /refreshtoken WITH prefix /titi with X-Forwarded-Proto=HTTPS)""" in {
    Get("http://www.example.com/").withHeaders(RawHeader("X-Forwarded-Proto","HTTPS")) ~> testRefreshTokenTiti ~> check {
      status shouldEqual StatusCodes.SeeOther
      val locationO: Option[Location] = header[Location]
      locationO shouldEqual(Some(Location("https://www.example.com/titi/refreshtoken")))
    }
  }

}