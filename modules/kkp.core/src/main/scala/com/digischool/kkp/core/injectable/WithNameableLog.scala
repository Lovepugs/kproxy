package com.digischool.kkp.core.injectable

import akka.event.LoggingAdapter

trait WithNameableLog {
  def logAs(logger: String): LoggingAdapter
  def logAs(c: Class[_]): LoggingAdapter = logAs(c.getCanonicalName)
}
