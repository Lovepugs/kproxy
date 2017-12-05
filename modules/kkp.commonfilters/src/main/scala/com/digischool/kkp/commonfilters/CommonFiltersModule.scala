package com.digischool.kkp.commonfilters

import akka.actor.{ActorSystem, ExtendedActorSystem, ExtensionId}
import com.digischool.kkp.commonfilters.filters.{RequestLogger, RequestTimeout, UriLogger}
import com.digischool.kkp.commonfilters.models._
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.configrepository.NoConf
import com.digischool.kkp.core.models.KModule
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.typesafe.config.Config


class CommonFiltersModule(kernel: KProxyKernel)(implicit system: ActorSystem) extends KModule[NoConf](kernel) with TrailingSlashRemover with RedirectHttps {

  val configurationKey = "CommonFiltersModule"

  private def cfc(config: Config) =
    getConfig(config).get

    def trailing(system: ActorSystem, config: Config) = trailingSlashRemover

    def redirectToHttps(system: ActorSystem, config: Config) = redirectHttps

  override def namedFunctions: KModule.PossibleFilters =
    Map(
      "SlashRemover"   -> CaseClassParser.unit(trailing _),
      "RedirectHttps"  -> CaseClassParser.unit(redirectToHttps _),
      CaseClassParser.named[UriLogger],
      CaseClassParser.named[RequestLogger],
      CaseClassParser.named[RequestTimeout]
    )

  override def onStart(): Unit = ()

  override def onStop(): Unit = ()

}

object CommonFiltersModule extends ExtensionId[CommonFiltersModule] {
  override def createExtension(system: ExtendedActorSystem): CommonFiltersModule =
    new CommonFiltersModule(KProxyKernel(system))(system)
}
