package com.digischool.kkp.core.models

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.Try

/**
  * A custom internal header, to make sure that we don't materialize the entity source twice.
  * Whenever entity is read, the header should be set to true
  */
case class DrainedHeader(drained: Boolean) extends ModeledCustomHeader[DrainedHeader] {
  override def companion = DrainedHeader

  override def value(): String = drained.toString

  override def renderInRequests() = true

  override def renderInResponses() = true

}

object DrainedHeader extends ModeledCustomHeaderCompanion[DrainedHeader] {
  override def name = "KPROXY-INTERNAL-DRAINED"

  override def parse(value: String): Try[DrainedHeader] = Try(value.toBoolean).map(apply)
}