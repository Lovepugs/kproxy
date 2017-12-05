package com.digischool.kkp

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.Supervision.Resume
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.digischool.kkp.core.configrepository.ConfigRepository
import com.digischool.kkp.core.services.RouteBuilder

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object KProxy extends App {

  implicit val system = ActorSystem("ProxySystem")
  private val log = Logging(system,"proxy")

  implicit val mat = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy { e =>
    log.error(e, "general error in stream")
    Resume
  })
  implicit val executor = system.dispatcher


  val config = system.settings.config
  val confName: String = config.getString("konf.name")
  val env: String = config.getString("proxy.env")

  log.info("Using conf " + confName)
  log.info("Environment " + env)

  val configs = ConfigRepository(system).source

  //TODO avoid rebinding all ports when only one changes (not important, since we bind ony one port for now)
  configs.
    //build routes
    mapAsync(1)(RouteBuilder(system).buildRoute).
    //keep track of which port is currently bound
    scan(Map.empty[Int, Option[Route]]){
      case (s, m) =>
        val toUnbind = s.filter(p => p._2.isDefined && !m.keySet.contains(p._1))
        m.mapValues(Some(_)) ++ toUnbind.mapValues(_ => None)
    }.
    mapConcat(identity).
    //work independently by port
    groupBy(Int.MaxValue, _._1).
    //keep track of the current binding on the port
    scanAsync(Option.empty[Http.ServerBinding]){
      //if the port is not bound, and we have a new route, bind it
      case (None, (port, Some(route))) => Http().bindAndHandle(Route.handlerFlow(route), "0.0.0.0", port).map(Some(_))
      //if the port is not bound, and there's not route, do nothing (should not happen)
      case (None, _) => Future.successful(None)
      //if the port is bound, and there is no route anymore for it, unbind it
      case (Some(binding), (_, None)) => binding.unbind().map(_ => None)
      //if the port is bound, and there is an updated route, unbind the old route, and bind the new one
      case (Some(binding), (port, Some(route))) => for {
        _ <- binding.unbind()
        newBinding <- Http().bindAndHandle(Route.handlerFlow(route), "0.0.0.0", port)
      } yield Some(newBinding)
    }.
    mapConcat(_.toList).
    mergeSubstreams.
    //log which bindings are restarted
    runForeach{ binding =>
      log.info(s"(Re)Starting to listen on port ${binding.localAddress.getPort}")
    }

  Await.result(system.whenTerminated, Duration.Inf)

}

