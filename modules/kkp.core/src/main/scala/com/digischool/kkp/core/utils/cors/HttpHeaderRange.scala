package com.digischool.kkp.core.utils.cors

import scala.collection.immutable.Seq

/**
  * Created by cyrille on 09/12/2016.
  */
sealed abstract class HttpHeaderRange {
  def matches(header: String): Boolean
}

object HttpHeaderRange {
  case object `*` extends HttpHeaderRange {
    def matches(header: String) = true
  }

  final case class Default(headers: Seq[String]) extends HttpHeaderRange {
    val lowercaseHeaders = headers.map(_.toLowerCase)
    def matches(header: String): Boolean = lowercaseHeaders contains header.toLowerCase
  }

  def apply(headers: String*): Default = Default(Seq(headers: _*))
}
