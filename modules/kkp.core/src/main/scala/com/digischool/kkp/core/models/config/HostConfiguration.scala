package com.digischool.kkp.core.models.config

import akka.http.scaladsl.model.Uri.Path
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive0, StandardRoute}
import com.digischool.kkp.core.directives.Forwarder
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.typesafe.config.Config
import play.api.libs.json.Json
import shapeless.LabelledGeneric

case class ServerDefinition(hosts:List[String], port:Int=80, rootPath:String="")

case class HostConfiguration(server: ServerDefinition,
                             routes  : List[String],
                             topFilter: String,
                             bottomFilter: String
                             ) {

  val getRoot = if (server.rootPath.isEmpty) Path.Empty else Path / server.rootPath
}

case class BackEnd(host: String, rootPath: String = "", port: Int = 0) extends CustomDirective {
  def cleanRootPath: String = Some(rootPath.trim).filterNot(_.isEmpty).getOrElse("")

  def forward(implicit system: ActorSystem) = StandardRoute(Forwarder.forward2(this))

  override def apply(system: ActorSystem, conf: Config): Directive0 = forward(system)
}

case class HeaderConfiguration(name: String, value: String)

object BackEnd{
  implicit val backEndFormat = Json.format[BackEnd]

  implicit val gen = LabelledGeneric[BackEnd]
  implicit val parser: CaseClassParser[BackEnd] = CaseClassParser.generic
}

object ServerDefinition{
  implicit val serverDefinitionFormat = Json.format[ServerDefinition]
}

object HeaderConfiguration{
  implicit val headerConfigurationFormat = Json.format[HeaderConfiguration]
}

object HostConfiguration{
  implicit val hostConfigurationFormat = Json.format[HostConfiguration]
}
