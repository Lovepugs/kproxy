package com.digischool.kkp.core.services

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId}
import com.digischool.kkp.core.KProxyKernel
import com.digischool.kkp.core.injectable.WithSystem
import com.digischool.kkp.core.models.KModule.PossibleFilters

import scala.concurrent.Future


trait ModuleProvider extends Extension {

  def fetchFilters(modules: List[String]): Future[PossibleFilters]

}

class ModuleProviderImpl(val kernel: KProxyKernel, val system: ActorSystem) extends WithSystem with ModuleProvider {
  private lazy val log = logAs("ModuleProvider")


  /**
    * FIXME
    * Ugly Tempory way to init modules
    * @param name
    */
  private def loadObject(name:String): Future[Option[KProxyKernel.AnyKModule]] = {
    import scala.reflect.runtime.universe
    Future{
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val module = runtimeMirror.staticModule(name)
      val obj = runtimeMirror.reflectModule(module)
      //log.info("LOADING MODULE NAME : " + name )
      val extId = obj.instance.asInstanceOf[ExtensionId[_ <: KProxyKernel.AnyKModule]]
      Some(extId(system))
    } recover {
      case e : Throwable =>
        log.error(e, s"error while loading $name")
        None
    }
  }

  def loadModules(modules: List[String]): Future[Boolean] =
    Future.traverse(modules)(n => loadObject(n+"$")).map(_.count(_.isDefined) == modules.size)

  private def checkModules(modules: List[String], i:Int = 0, max:Int = 10): Future[Boolean] = {

    val modulesF = for {
      _ <- loadModules(modules)
      modules <- kernel.modules()
    } yield modules.keySet

    val expectedModuleNames: Set[String] = modules.toSet

    modulesF.flatMap{ moduleNames =>
      val missingModules = expectedModuleNames.diff(moduleNames)
      if(missingModules.isEmpty || i == max){
        Future.successful(missingModules.isEmpty)
      }else{
        log.info(s"INIT Modules n° ${i+1}; waiting for: \n\t\t" + missingModules.mkString("\n\t\t"))
        checkModules(modules, i+1 , max) // try again
      }  //check if all expected are present
    }
  }

  def fetchFilters(modules: List[String]): Future[PossibleFilters] = for {
    _ <- checkModules(modules) //check and register modules
    kModules <- kernel.modules().map(_.values.toList)
  } yield kModules.map(_.namespacedFunctions).fold(Nil)(_ ++ _).groupBy(_._1).collect{
    case (n, fns) if fns.lengthCompare(1) == 0 => n -> fns.head._2
  }
}

case class FakeModuleProvider(filters: PossibleFilters) extends ModuleProvider {
  override def fetchFilters(modules: List[String]): Future[PossibleFilters] = Future.successful(filters)
}

object ModuleProvider extends ExtensionId[ModuleProvider]{
  override def createExtension(system: ExtendedActorSystem): ModuleProvider = new ModuleProviderImpl(KProxyKernel(system), system)
}