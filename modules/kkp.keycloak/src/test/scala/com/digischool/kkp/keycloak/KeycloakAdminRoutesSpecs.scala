package com.digischool.kkp.keycloak

import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.digischool.kkp.core.configrepository.impl.TestConfigRepository
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.core.services.{DirectiveInterpreter, ModuleProviderImpl, RouteBuilderImpl}
import com.digischool.kkp.core.{ConstantKProxyKernel, CoreModule}
import com.typesafe.config.{Config, ConfigFactory}
import org.keycloak.adapters.KeycloakDeployment
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * Created by fred on 06/10/2015.
 */
object FakeDirectiveInterpreter extends DirectiveInterpreter {
  override def interpretCustom: PartialFunction[CustomDirective, CustomDirective] = {
    case _ => CustomDirective((_, _) => Directives.pass)
  }
}

class KeycloakAdminRoutesSpecs extends WordSpecLike with MustMatchers with ScalatestRouteTest {
  val config :Config = ConfigFactory.empty()


  val loadedKernel = new ConstantKProxyKernel(system, CoreModule, KeycloakAuthenticateModule)
  val routeBuilder = new RouteBuilderImpl(loadedKernel, new ModuleProviderImpl(loadedKernel, system), FakeDirectiveInterpreter)(system)

  val dummyKeycloakDeployment = new KeycloakDeployment()
  import Directives._

  lazy val allroutes = Await.result(routeBuilder.buildRoute(TestConfigRepository.getModules, Map()), 1.second).values.fold(routeBuilder.lifepage)(_ ~ _)

  val adminRoute: Route = KeycloakAuthenticateModule(system).adminRoutes(dummyKeycloakDeployment)


  """Keycloak admin routes should""" should {

    """be handled for POST /callback/k_logout""" in {
      Post("/callback/k_logout") ~> adminRoute ~> check {
        handled mustBe true
      }
    }


    """be handled for POST /callback/k_logout in all routes""" in {
      Post("/callback/k_logout") ~> Host("titi.lo", 8181) ~> allroutes ~> check {
//        handled mustBe true
        pending
      }
    }
  }



}