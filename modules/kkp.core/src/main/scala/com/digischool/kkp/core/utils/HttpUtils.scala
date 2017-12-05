package com.digischool.kkp.core.utils

import akka.Done
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Rejection, RejectionHandler}
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshal, Unmarshaller}
import akka.stream.Materializer
import com.digischool.kkp.core.models.DrainedHeader

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait HttpUtils {
  /**
    * @param httpResponse
    * @return Future[Done]
    *         Helper method used to drain an httpResponse if you're not interested in the body
    *         This is used to prevent Stalle connection if you forget to drain the datas
    */
  def drain(httpResponse: HttpResponse)(implicit mat: Materializer, executor: ExecutionContext): Future[Done] = {
    if (httpResponse.status.allowsEntity()) {
      httpResponse.discardEntityBytes().future()
    } else {
      Future.successful(Done)
    }
  } //you have to drain or the connection will stall

  val drainRequest: Directive0 = extractMaterializer flatMap { implicit mat =>
    mapRequest { req =>
      if (!req.header[DrainedHeader].exists(_.drained))
        req.discardEntityBytes()
      req.removeHeader(DrainedHeader.name).addHeader(DrainedHeader(true))
    }
  }

  def flagDrainedEntity[T](un: FromRequestUnmarshaller[T]) = mapRequest(_.removeHeader(DrainedHeader.name).addHeader(DrainedHeader(true))) & entity(un)

  val drainResponse = extractMaterializer flatMap { implicit mat =>
    mapResponse { resp =>
      if (!resp.header[DrainedHeader].exists(_.drained))
        resp.discardEntityBytes()
      resp.removeHeader(DrainedHeader.name).addHeader(DrainedHeader(true))
    }
  }

  def requestAndDrain(request: HttpRequest)(implicit http: HttpExt, fm: Materializer, executor: ExecutionContext): Future[HttpResponse] =
    http.singleRequest(request).andThen {
      case Success(s) => drain(s)
    }

  def keepRequestAndUnmarshal[T](request: HttpRequest)(implicit http: HttpExt, fm: Materializer, executor: ExecutionContext, um: Unmarshaller[HttpResponse, T]) =
    for {
      resp <- http.singleRequest(request)
      entity <- Unmarshal(resp).to[T].andThen { case Failure(_) => drain(resp) }
    } yield (resp, entity)

  def requestAndUnmarshal[T](request: HttpRequest, expectedStatusCode: Option[StatusCode] = None, strictTimeout: FiniteDuration = 3.seconds)(implicit http: HttpExt, fm: Materializer, executor: ExecutionContext, um: Unmarshaller[ResponseEntity, T]) =
    for {
      resp <- http.singleRequest(request)
      strictEntity <- resp.entity.toStrict(strictTimeout)
      _ = if (expectedStatusCode.contains(resp.status)) () else {
        throw new IllegalArgumentException(s"Unexpected response status code for request\n$request\n${resp.status}")
      }
      entity <- Unmarshal(strictEntity).to[T]
    } yield entity

  def requestAndUnmarshalOK[T](request: HttpRequest)(implicit http: HttpExt, fm: Materializer, executor: ExecutionContext, um: Unmarshaller[ResponseEntity, T]) =
    requestAndUnmarshal(request, Some(StatusCodes.OK))

  private val drainingRejectionHandler = RejectionHandler.newBuilder().handleAll[Rejection] { rejs =>
    val defaultHandled = handleRejections(RejectionHandler.default)(reject(rejs: _*))
    drainRequest {
      defaultHandled
    }
  }.result()

  val drainRecoverer: Directive0 = handleRejections(drainingRejectionHandler)
}

object HttpUtils extends HttpUtils