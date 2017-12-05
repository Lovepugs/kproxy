package com.digischool.kkp.core.configrepository

import akka.NotUsed
import akka.actor.{ExtendedActorSystem, Extension, ExtensionId}
import akka.event.Logging
import akka.stream.scaladsl.Source
import com.digischool.kkp.core.configrepository.impl.{FileSystemConfigRepository, GitFSConfigRepository, HookedGitFSConfigRepository}
import com.digischool.kkp.core.configrepository.model.ModularConfig
import com.kreactive.brigitte.Fetcher

import scala.language.implicitConversions
import scala.util.Try

/**
  * A way to get all kProxyConfig needed to configure the proxy running on the system
  */
trait ConfigRepository extends Extension with Fetcher[ModularConfig] {
  implicit def source2notUsed[In, M](s: Source[In, M]): Source[In, NotUsed] = s.mapMaterializedValue(_ => NotUsed)
}

object ConfigRepository extends ExtensionId[ConfigRepository] {
  override def createExtension(system: ExtendedActorSystem): ConfigRepository = {
    lazy val log = Logging(system, getClass)
    val env = system.settings.config.getString("proxy.env")
    if (env == "dev") {
      // dev default is FileSystem
      log.info("Dev mode, loading file system config")
      FileSystemConfigRepository(system)
    } else
    // default is Git with webhook, which fallbacks to git without hook (pull only)
      Try(HookedGitFSConfigRepository(system)).recover {
        case e => log.error(e, "Unable to load HookedGitFSConfigRepository")
          GitFSConfigRepository(system)
      }.recover {
        case e => log.error(e, "Unable to load GitFSConfigRepository")
          FileSystemConfigRepository(system)
      }.get
  }
}