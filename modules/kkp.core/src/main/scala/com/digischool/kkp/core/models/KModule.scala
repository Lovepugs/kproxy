package com.digischool.kkp.core.models

import akka.actor.{ActorSystem, Extension}
import akka.http.scaladsl.server.Directive0
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.configrepository.KProxyConfig
import com.digischool.kkp.core.injectable.WithSystem
import com.digischool.kkp.core.models.config.HostConfiguration
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.typesafe.config.{Config, ConfigException}
import configs.Configs
import configs.syntax._

import scala.language.implicitConversions
import scala.util.Try

/**
  * Extensions extending this trait must have an ExtensionId as companion object. eg,
  * <code>
  *   class MyModule(implicit val system: ActorSystem) extends KModule[MyConfig]
  *
  *   object MyModule extends ExtensionId[MyModule]
  * </code>
  */
abstract class KModule[T <: KProxyConfig](kernel: KProxyKernel)(implicit val system: ActorSystem, configs: Configs[T]) extends Extension with WithSystem {

  import KModule._

  private lazy val log = logAs("KModule")

  def configurationKey: String

  def namedFunctions: PossibleFilters

  def namespacedFunctions = namedFunctions.flatMap{
    case (k, v) => Map(k -> v, s"$name.$k" -> v)
  }.toList

  def onStart(): Unit

  def onStop(): Unit

  def getConfig(config: Config): Try[T] = Try(config.get[T](configurationKey)).map(_.value).filter(_.isValid).recover{
    case e => throw new ConfigException(s"${getClass.getSimpleName} not available at $configurationKey for ${config.get[List[String]]("server.hosts").value.head}", e) {}
  }

  def register() = kernel.addModule(name, this)

  def name: String = this.getClass.getCanonicalName

  register()

  implicit def func2custom(f: (ActorSystem, Config) => Directive0): CustomDirective = CustomDirective(f)

}

object KModule {
  type PossibleFilters = Map[String, CaseClassParser[CustomDirective]]
  def getHostConfig(config: Config): HostConfiguration = config.extract[HostConfiguration].
    valueOrThrow(e => new Exception(s"unable to extract HostConfig for ${config.get[List[String]]("server.hosts").value.head}", e.configException))
}
