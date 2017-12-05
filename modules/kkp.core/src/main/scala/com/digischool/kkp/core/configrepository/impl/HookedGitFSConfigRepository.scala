package com.digischool.kkp.core.configrepository.impl

import akka.actor.{ActorSystem, ExtendedActorSystem, ExtensionId}
import akka.event.{Logging, LoggingAdapter}
import better.files._
import com.digischool.kkp.core.configrepository.ConfigRepository
import com.kreactive.brigitte.impl.git.GitConfig
import com.kreactive.brigitte.impl.git.webhook.HookedGitFSFetcher

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * The extensionId for default implementation of [[HookedGitFSConfigRepository]], using system conf:
  *  - base folder is `konf.folder` (absolute path) if specified, otherwise /tmp/git-repo
  *  - init folder is `konf.folder.init` (absolute path)
  *  - delay is 5 seconds
  *  - git config remote (as [[com.kreactive.brigitte.impl.git.GitRemote]] object) is `konf.git.remote` (if no remote is set, falls back to [[GitFSConfigRepository]])
  *  - git tracked branch is `konf.git.branch`
  *  - port is `konf.git.hook.port` or 9090 by default
  */
object HookedGitFSConfigRepository extends ExtensionId[ConfigRepository] {
  val HOOK_PATH = "hook/gitlab"
  val HOOK_PORT = 9090

  override def createExtension(system: ExtendedActorSystem): ConfigRepository = {
    import configs.syntax._
    implicit val log: LoggingAdapter = Logging(system, getClass)
    implicit val executor: ExecutionContext = system.dispatcher
    implicit val s: ActorSystem = system
    val config = system.settings.config
    val folder = config.get[String]("konf.folder").toOption.map(_.toFile)
    val folderInit = config.get[String]("konf.folder.init").toOption.map(_.toFile)
    val gitConf = config.get[GitConfig]("konf.git").value

    log.info("Starting a HookedGitFsConfigRepository...")
    val listeningPort = config.get[Int]("konf.git.hook.port").valueOrElse {
      log.warning(s"No port specified for webhooks; will listen on $HOOK_PORT")
      HOOK_PORT
    }
    val hookPath = config.get[String]("konf.git.hook.path").valueOrElse {
      log.warning(s"No path specified for webhooks; will listen on $HOOK_PATH")
      HOOK_PATH
    }
    gitConf.remote.flatMap { r =>
      Try(new HookedGitFSFetcher(listeningPort, hookPath, gitConf.branch, r, folder, tempFolder = GitFSConfigRepository.TEMP_FILE, initFolder = folderInit, fetcher = FileSystemConfigRepository.fetcher) with ConfigRepository).fold(
        { e =>
          log.error(e, "Error while trying a HookedGitFSConfigRepository")
          None
        },
        Some(_)
      )
    }.getOrElse {
      log.warning("Unable to use a HookedGitFsConfigRepository")
      GitFSConfigRepository(system)
    }
  }
}