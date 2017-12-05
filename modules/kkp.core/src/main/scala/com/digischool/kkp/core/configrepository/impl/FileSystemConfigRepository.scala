package com.digischool.kkp.core.configrepository.impl

import akka.actor.{ExtendedActorSystem, ExtensionId}
import akka.event.{Logging, LoggingAdapter}
import better.files._
import com.digischool.kkp.core.configrepository.ConfigRepository
import com.digischool.kkp.core.configrepository.model.ModularConfig
import com.kreactive.brigitte.impl.FileSystemFetcher
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Implementation of [[ConfigRepository]] for file system based config.
  * The config file are the `.konf` files in a base folder or one of its (recursive) children.
  * The modules are the rows of the file named `modules` on the base folder
  * Everything is checked for changes with a repeated scheduler
  *
  * @param delay      how often the files are checked for changes
  * @param folder     the root folder
  * @param initFolder a docker hack (I don't know what it really means @cyrille.corpet)
  *
  *
  *  - base folder is `konf.folder` (absolute path)
  *  - init folder is `konf.folder.init` (absolute path)
  *  - delay is 5 seconds
  */
object FileSystemConfigRepository extends ExtensionId[ConfigRepository] {

  def buildConfig(folder: File)(implicit executor: ExecutionContext): Future[List[(String, Config)]] = {
    val konfFiles = folder.glob("**/*.konf")(visitOptions = File.VisitOptions.follow).toList
    Future.traverse(konfFiles) { f =>
      Future((f.pathAsString, ConfigFactory.parseFile(f.toJava).resolve()))
    }.map { configs =>
      if (configs.isEmpty) throw new IllegalArgumentException("No *.konf file found at path : " + folder)
      configs
    }
  }

  def getModules(folder: File): List[String] =
    Try((folder / "modules").lines.toList.filterNot(_.trim.isEmpty)).getOrElse(Nil)

  def fetcher(folder: File)(implicit executor: ExecutionContext): Future[ModularConfig] =
    buildConfig(folder).map(cfgs => ModularConfig(cfgs.toMap, getModules(folder)))

  def createExtension(system: ExtendedActorSystem): ConfigRepository = {
    implicit val executor: ExecutionContext = system.dispatcher
    implicit val log: LoggingAdapter = Logging(system, getClass)
    val config = system.settings.config
    log.info("Starting a FileSystemFetcher...")
    val folder = config.getString("konf.folder").toFile
    log.info(s"Local config searched in ${folder.pathAsString}")
    val folderInit = Try(config.getString("konf.folder.init").toFile).toOption
    val repo = new FileSystemFetcher[ModularConfig](5.seconds, folder, folderInit, fetcher) with ConfigRepository
    repo.init(system.settings.config.getString("proxy.env"))
    repo
  }
}


