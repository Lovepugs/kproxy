package com.digischool.kkp.core.configrepository.impl

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.event.{Logging, LoggingAdapter}
import better.files._
import com.digischool.kkp.core.configrepository.ConfigRepository
import com.kreactive.brigitte.impl.git.{GitConfig, GitFSFetcher, GitRemote}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

/**
  * The extensionId for default implementation of [[GitFSConfigRepository]], using system conf:
  *  - base folder is `konf.folder` (absolute path) if specified, otherwise /tmp/git-repo
  *  - init folder is `konf.folder.init` (absolute path)
  *  - delay is 5 seconds
  *  - git config remote (as [[GitRemote]] object) is `konf.git.remote`
  *  - git tracked branch is `konf.git.branch`
  */
object GitFSConfigRepository extends ExtensionId[ConfigRepository] with ExtensionIdProvider {
  val TEMP_FILE = "/tmp/git-repo".toFile

  override def createExtension(system: ExtendedActorSystem): ConfigRepository = {
    import configs.syntax._
    implicit val log: LoggingAdapter = Logging(system, getClass)
    implicit val executor: ExecutionContext = system.dispatcher
    val config = system.settings.config
    val folder = config.get[String]("konf.folder").toOption.map(_.toFile)
    val folderInit = Try(config.getString("konf.folder.init").toFile).toOption
    log.info("Starting a GitFSConfigRepository...")
    val gitConf = config.get[GitConfig]("konf.git").value
    if (gitConf.remote.isEmpty) log.info(s"Using local git repository on branch ${gitConf.branch}")
    if (folder.isEmpty && gitConf.remote.isDefined)
      log.info("Resetting local repository in temp folder")
    new GitFSFetcher(gitConf.branch, gitConf.remote, 5.seconds, folder, tempFolder = TEMP_FILE, initFolder = folderInit, fetcher = FileSystemConfigRepository.fetcher) with ConfigRepository
  }

  override def lookup(): ExtensionId[_ <: Extension] = this
}