package com.digischool.kkp.commonfilters.models

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.digischool.kkp.core.utils.{HttpUtils, UriHelper}


trait RedirectHttps extends UriHelper {
  val redirectHttps = RedirectHttps(StatusCodes.MovedPermanently)

  private def RedirectHttps(redirectionType: StatusCodes.Redirection): Directive0 =
    extractRequest.flatMap { req ⇒
      if (withScheme(req).equalsIgnoreCase("http")) {
        val newUri = req.uri.withScheme("https")
        HttpUtils.drainRequest & redirect(newUri, redirectionType)
      } else pass
    }

}
