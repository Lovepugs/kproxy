package com.digischool.kkp.core
package admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.digischool.kkp.core.directives.BasicHttpAuthDirective
import play.api.libs.json.Json


trait ProxyAdminEndPoint extends BasicHttpAuthDirective {

  val matcher: PathMatcher0 = "proxyadmin" / "lifepage"

  val lifepage: Route = get{
    import com.digischool.kkp.core.utils.PlayJsonSupport._
    path(matcher) {
          complete{
            Json.obj(
              "name" -> BuildInfo.name,
              "version" -> BuildInfo.version,
              "scalaVersion" -> BuildInfo.scalaVersion,
              "sbtVersion" -> BuildInfo.sbtVersion,
              "git commit" -> BuildInfo.gitHeadCommit)
        }
    }
  }
}
