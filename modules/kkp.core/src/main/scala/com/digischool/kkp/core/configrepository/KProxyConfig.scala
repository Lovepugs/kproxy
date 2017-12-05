package com.digischool.kkp.core.configrepository

import com.digischool.kkp.core.injectable.WithNameableLog

trait KProxyConfig {

  def isValid(implicit system: WithNameableLog): Boolean

}
