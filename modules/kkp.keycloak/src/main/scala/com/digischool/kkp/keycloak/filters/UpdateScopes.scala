package com.digischool.kkp.keycloak.filters

import akka.actor.ActorSystem
import akka.event.Logging.LogLevel
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.{FormData, Uri}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.digischool.kkp.core.models.CustomDirective
import com.digischool.kkp.dsl2.routedsl.CaseClassParser
import com.digischool.kkp.keycloak.KeycloakAuthenticateModule
import com.typesafe.config.Config
import org.keycloak.OAuth2Constants
import org.keycloak.adapters.KeycloakDeployment
import shapeless.LabelledGeneric

import scala.concurrent.duration._

/**
  * A directive to add scopes for keycloak authentication or token retrieval
  * @param add
  * @param remove
  */
case class UpdateScopes(add: String = "", remove: String = "") extends CustomDirective {

  import UpdateScopes._
  override def apply(v1: ActorSystem, v2: Config): Directive0 = {
    val deployment = KeycloakAuthenticateModule(v1).getConfig(v2).get.deployment
    authRouteInject(deployment, add, remove) | tokenRouteInject(deployment, add, remove) | pass
  }
}

object UpdateScopes {
  implicit val gen = LabelledGeneric[UpdateScopes]
  implicit val parser: CaseClassParser[UpdateScopes] = CaseClassParser.generic

  def authRouteInject(deployment: KeycloakDeployment, add: String, remove: String) =
    rawPathPrefix(PathMatcher(Path(deployment.getAuthUrl.build().getRawPath), ())) &
      mapRequest { req =>
        req.withUri(req.uri.withQuery(newQuery(req.uri.query(), add, remove)))
      }

  def tokenRouteInject(deployment: KeycloakDeployment, add: String, remove: String) =
    rawPathPrefix(PathMatcher(Uri(deployment.getTokenUrl).path, ())) &
      toStrictEntity(10.seconds) & entity(as[FormData]) flatMap { entity =>
      mapRequest { req =>
        req.withEntity(FormData(newQuery(entity.fields, add, remove)).toEntity)
      }
    }

  def newQuery(oldQuery: Query, add: String, remove: String): Query = {
    val query = oldQuery.toMultiMap
    val neededScopes = query.getOrElse(OAuth2Constants.SCOPE, Nil).flatMap(_.split(" ")).toSet -- remove.split(" ") ++ add.split(" ")
    val withScope = query + (OAuth2Constants.SCOPE -> List(neededScopes.mkString(" ")))
    Query(withScope.toSeq.flatMap { case (a, bs) => bs.map((a, _)) }: _*)
  }
}
