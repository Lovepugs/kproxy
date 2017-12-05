package com.digischool.kkp.core.injectable

import com.digischool.kkp.core.KProxyKernel

trait WithKernel {
  implicit def kernel: KProxyKernel
}