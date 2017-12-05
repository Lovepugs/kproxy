package com.digischool.kkp.core.injectable

import akka.http.scaladsl.HttpExt

trait WithHttpExt {
  implicit def http: HttpExt
}