package com.digischool.kkp.core.injectable

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import com.digischool.kkp.core.KProxyKernel

trait WithSystem extends WithMaterializer with WithHttpExt with WithNameableLog with WithExecutionContext {

  implicit val nameableLog = this
  implicit def system: ActorSystem
  implicit lazy val mat: Materializer = ActorMaterializer()
  implicit lazy val executor = system.dispatcher

  def logAs(s: String) = Logging(system, s)
  lazy val http = Http()
}