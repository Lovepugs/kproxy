package com.digischool.kkp.core

import akka.actor.{ActorSystem, ExtendedActorSystem, ExtensionId}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive0, Directives}
import com.digischool.kkp.core.configrepository.NoConf
import com.digischool.kkp.core.models.KModule
import com.digischool.kkp.core.models.config.{BackEnd, Headers, Redirect}
import com.digischool.kkp.core.utils.HttpUtils
import com.digischool.kkp.core.utils.cors.CorsUtils
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.typesafe.config.Config

class CoreModule(kernel: KProxyKernel)(implicit system: ActorSystem) extends KModule[NoConf](kernel) {
  override def configurationKey: String = ""

  override def onStart(): Unit = ()

  override def onStop(): Unit = ()

  override def namedFunctions: KModule.PossibleFilters =
    Map(CaseClassParser.named[Headers],
      CaseClassParser.named[Redirect],
      CaseClassParser.named[BackEnd],
      "Deny" -> CaseClassParser.unit(CoreModule.deny _),
      "NotFound" -> CaseClassParser.unit(CoreModule.notFound _),
      "Cors" -> CaseClassParser.unit((_: ActorSystem, _: Config) => CorsUtils.cors())
    )
}                     

object CoreModule extends ExtensionId[CoreModule] {
  override def createExtension(system: ExtendedActorSystem): CoreModule = new CoreModule(KProxyKernel(system))(system)

  def deny(v1: ActorSystem, v2: Config): Directive0 =
    HttpUtils.drainRequest & Directives.complete(StatusCodes.Unauthorized)

  def notFound(v1: ActorSystem, v2: Config): Directive0 =
    HttpUtils.drainRequest & Directives.complete(StatusCodes.NotFound)
}
