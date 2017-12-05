package com.digischool.kkp.core.injectable

import scala.concurrent.ExecutionContext

trait WithExecutionContext {
  implicit def executor: ExecutionContext
}