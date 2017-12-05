package com.digischool.kkp.core

import akka.actor.{Actor, ActorRefFactory, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import com.digischool.kkp.core.KProxyKernel.AnyKModule
import com.digischool.kkp.core.configrepository.KProxyConfig
import com.digischool.kkp.core.models.KModule

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

trait KProxyKernel extends Extension {
  def modules(): Future[Map[String, AnyKModule]]
  def addModule(name:String, module:AnyKModule): Unit
  def addModules(entries : Map[String, AnyKModule]): Unit
  def clearModules(): Unit
  def getModule(name:String): Future[Option[AnyKModule]]
}

class KProxyKernelImpl(system: ActorRefFactory)(implicit mat: ActorMaterializer, executor: ExecutionContext, timeout: Timeout) extends KProxyKernel {
  import RegistryActor._

  private val registry = system.actorOf(RegistryActor.props)

  def modules(): Future[Map[String, AnyKModule]] = (registry ? GetAll).mapTo[Map[String, AnyKModule]]

  def addModule(name:String, module:AnyKModule): Unit = registry ! AddModule(name, module)

  def addModules(entries : Map[String, AnyKModule]): Unit = registry ! AddModules(entries)

  def clearModules(): Unit = registry ! ClearModules

  def getModule(name:String): Future[Option[AnyKModule]] = (registry ? Get(name)).mapTo[Option[AnyKModule]]

}

class ConstantKProxyKernel(system: ActorSystem, val extIds: List[ExtensionId[_ <:AnyKModule]]) extends KProxyKernel {
  def this(system: ActorSystem, ids:ExtensionId[_ <:AnyKModule]* ) = this(system, ids.toList)

  val loaded: Map[String, AnyKModule] =
    extIds.map(id => id.getClass.getCanonicalName.dropRight(1) -> id(system)).toMap

  override def modules(): Future[Map[String, AnyKModule]] = Future.successful(loaded)

  override def addModule(name: String, module: AnyKModule): Unit = ()

  override def addModules(entries: Map[String, AnyKModule]): Unit = ()

  override def clearModules(): Unit = ()

  override def getModule(name: String): Future[Option[AnyKModule]] = Future.successful(loaded.get(name))
}

object KProxyKernel extends ExtensionId[KProxyKernel] with ExtensionIdProvider {
  type AnyKModule = KModule[Conf] forSome {type Conf <: KProxyConfig}

  override def createExtension(system: ExtendedActorSystem): KProxyKernel =
    new KProxyKernelImpl(system)(ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider))(system), system.dispatcher, 5.seconds)

  override def lookup(): ExtensionId[_ <: Extension] = this
}

class RegistryActor extends Actor {
  import RegistryActor._
  lazy val log = Logging(context.system, "Module Registry")
  val modules = mutable.Map.empty[String, AnyKModule]

  val receive: Receive = {
    case AddModule(name, module) => modules += (name -> module)
    case AddModules(entries) => modules ++= entries
    case ClearModules => modules.clear()


    case Get(name) => sender ! modules.get(name)
    case GetAll => sender ! Map() ++ modules
  }
}

object RegistryActor {
  case class AddModule(name: String, module: AnyKModule)
  case class AddModules(entries: Map[String, AnyKModule])
  case object ClearModules
  case object GetAll
  case class Get(name: String)

  def props = Props(new RegistryActor)
}