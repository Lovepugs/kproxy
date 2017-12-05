package com.digischool.kkp.core.directives

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Authority, Host}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import akka.stream.ActorMaterializer
import com.digischool.kkp.core.models.DrainedHeader
import com.digischool.kkp.core.models.config.BackEnd
import com.digischool.kkp.core.utils.HttpUtils

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Forwarder extends HttpUtils {

  def forward2(targetPath: BackEnd)(implicit system: ActorSystem): Route =
    targetDirective(targetPath) {
      extractRequest { request =>
        val futureResponse: Future[HttpResponse] = Http().singleRequest(request.removeHeader(DrainedHeader.name).removeHeader("timeout-access"))(ActorMaterializer())
        onComplete(futureResponse) {
          case Success(resp) => complete(resp)
          case Failure(err) => drainRequest & complete(StatusCodes.InternalServerError, s"An error occurred: ${err.getMessage}")
        }
      }
    }

  private def targetDirective(baseUri: Uri) =
    mapRequest(r => r.copy(uri = r.uri.copy(authority = baseUri.authority, scheme = baseUri.scheme)))


  private def targetDirective(tp: BackEnd): Directive0 =
    extractRequest flatMap (r => targetDirective(updateUri(r, tp)))

  private def updateUri(r: HttpRequest, tp: BackEnd): Uri = {
    val auth: Authority = r.uri.authority.copy(host = Host(tp.host), port = tp.port)
    r.uri.copy(authority = auth)
  }
}