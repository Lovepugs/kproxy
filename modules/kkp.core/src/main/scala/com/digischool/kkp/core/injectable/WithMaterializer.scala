package com.digischool.kkp.core.injectable

import akka.stream.Materializer

trait WithMaterializer {
  implicit def mat: Materializer
}