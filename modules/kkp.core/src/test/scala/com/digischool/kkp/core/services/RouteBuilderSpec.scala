package com.digischool.kkp.core.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.dsl2.utils.validation.{Applicative, Validation}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shapeless.LabelledGeneric

import scala.util.Try

case class FakeLogged(roles: List[String] = Nil) extends CustomDirective {
  override def apply(v1: ActorSystem, v2: Config): Directive0 = complete(StatusCodes.Unauthorized)
}

object FakeLogged {
  implicit val gen = LabelledGeneric[FakeLogged]
  implicit val parser: CaseClassParser[FakeLogged] = CaseClassParser.generic
}

class RouteBuilderSpec extends WordSpecLike with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  type TryValid[+T] = Try[Validation[T]]
  val TryValid = Applicative[TryValid]

  val routeBuilder = RouteBuilder(system).asInstanceOf[RouteBuilderImpl]
  "a route builder" should {
    "properly parse the following directive: ALL     /access/super-admin                   \"Logged([super-admin])\"" in {
      val config = ConfigFactory.parseString(
        """
          |server {
          | hosts = ["hello.world"]
          |}
          |
          |topFilter = ""
          |bottomFilter = ""
          |
          |routes = [
          |        OPTIONS /
          |        ALL     /access/super-admin                   "Logged([super-admin])"
          |]
        """.stripMargin)
      val filters = Map("Logged" -> FakeLogged.parser, "Allow" -> CaseClassParser.unit(CustomDirective((_, _) => pass)))
      val (_, dir) = routeBuilder.prepareConf(("test", config), filters).get.get

      Get("http://hello.world/access/super-admin/") ~> dir {
        complete(StatusCodes.NotFound)
      } ~> check {

        status shouldEqual StatusCodes.Unauthorized
      }


    }
  }

}
