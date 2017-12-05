package com.digischool.kkp.commonfilters.models

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.digischool.kkp.core.utils.HttpUtils


trait TrailingSlashRemover {
  val trailingSlashRemover = redirectToNoTrailingSlashIfPresentButNotRoot(StatusCodes.MovedPermanently)

  private def redirectToNoTrailingSlashIfPresentButNotRoot(redirectionType: StatusCodes.Redirection): Directive0 =
    extractUri.flatMap { uri ⇒
      if (uri.path != Path.SingleSlash && uri.path.endsWithSlash) {
        val newPath = uri.path.reverse.tail.reverse
        val newUri = uri.withPath(newPath)
        HttpUtils.drainRequest & redirect(newUri, redirectionType)
      } else pass
    }

}
