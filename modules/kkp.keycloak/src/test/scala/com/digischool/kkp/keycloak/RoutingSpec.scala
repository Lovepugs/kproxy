package com.digischool.kkp.keycloak

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


trait RoutingSpec {

  val completeOk = complete("Ok")

  def echoComplete[T]: T ⇒ Route = { x ⇒ complete(x.toString) }
  def echoComplete2[T, U]: (T, U) ⇒ Route = { (x, y) ⇒ complete(s"$x $y") }

  val echoUnmatchedPath = extractUnmatchedPath { echoComplete }
  def echoCaptureAndUnmatchedPath[T]: T ⇒ Route =
    capture ⇒ ctx ⇒ ctx.complete(capture.toString + ":" + ctx.unmatchedPath)

}


