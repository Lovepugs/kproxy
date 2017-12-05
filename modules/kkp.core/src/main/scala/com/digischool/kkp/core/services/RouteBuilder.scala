package com.digischool.kkp.core.services

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.admin.ProxyAdminEndPoint
import com.digischool.kkp.core.configrepository.model.ModularConfig
import com.digischool.kkp.core.directives._
import com.digischool.kkp.core.injectable.WithSystem
import com.digischool.kkp.core.models.KModule.PossibleFilters
import com.digischool.kkp.core.models.config.ServerDefinition
import com.digischool.kkp.core.models.{CustomDirective, KModule}
import com.digischool.kkp.core.utils.HttpUtils
import com.digischool.kkp.dsl2.models.Directive
import com.digischool.kkp.dsl2.utils.validation.{Applicative, Validation}
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.util.Try

trait RouteBuilder extends Extension {

  def buildRoute(modules: List[String], confs: Map[String, Config]): Future[Map[Int, Route]]

  def buildRoute(modularConfig: ModularConfig): Future[Map[Int, Route]] = buildRoute(modularConfig.modules, modularConfig.confFiles)

}

class RouteBuilderImpl(val kernel: KProxyKernel, moduleProvider: ModuleProvider, di: DirectiveInterpreter)(implicit val system: ActorSystem) extends RouteBuilder with ProxyAdminEndPoint with WithSystem {

  import Directives._
  private lazy val log = logAs("RouteBuilder")

  type TryValid[+T] = Try[Validation[T]]
  val TryValid = Applicative[TryValid]
  import TryValid._

  def parseDirective(row: String, filters: PossibleFilters): TryValid[CustomDirective] =
    if (row.isEmpty) unit(CustomDirective.monoid.identity)
    else Directive.parse(row, filters).map(_.groupErrors(row)) |-> di.fold

  def prepareConf(conf: (String, Config), filters: PossibleFilters): TryValid[(Int, Directive0)] = {
    val hc = KModule.getHostConfig(conf._2)
    val validatedDir = Applicative.traverse[TryValid](hc.topFilter :: hc.bottomFilter :: hc.routes)(parseDirective(_, filters)) |-> (_.map(_(system, conf._2))) |-> {
      case topFilter :: bottomFilter :: directives =>
        sourceDirective(hc.server) & topFilter & directives.foldRight(complete(StatusCodes.NotFound): Directive0)(_ | _) & bottomFilter
    } map (_.groupErrors(s"In file ${conf._1}:"))
    validatedDir |-> (r => (hc.server.port, HttpUtils.drainRecoverer & r))
  }

  def prepareConfs(confs: Map[String, Config], providedFilters: PossibleFilters) = {
    Applicative.traverse[TryValid](confs)(prepareConf(_, providedFilters)) |-> (_.groupBy(_._1).mapValues(_.foldLeft(StandardRoute(lifepage): Directive0)(_ | _._2) (lifepage)))
  }

  def buildRoute(modules: List[String], confs: Map[String, Config]): Future[Map[Int, Route]] =
    moduleProvider.fetchFilters(modules).map{ filters =>
      log.info("available filters:\n\n" + filters.keySet.mkString("\n"))
      prepareConfs(confs, filters).get.get
    }

  private def sourceDirective(server: ServerDefinition): Directive0 = host(server.hosts: _*) & PortDirective.port(server.port) & rootPath(server.rootPath)

  private def rootPath(rootPath: String) = if (rootPath.isEmpty) pass else pathPrefix(rootPath)

}

object RouteBuilder extends ExtensionId[RouteBuilder] {
  override def createExtension(s: ExtendedActorSystem): RouteBuilder = new RouteBuilderImpl(KProxyKernel(s), ModuleProvider(s), DefaultDirectiveInterpreter)(s)
}
