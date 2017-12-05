package com.digischool.kkp.core.configrepository

import com.digischool.kkp.core.injectable.WithNameableLog
import configs.Configs

/**
  * Created by cyrille on 26/04/2016.
  */
sealed trait NoConf extends KProxyConfig
case object NoConf extends NoConf {
  override def isValid(implicit system: WithNameableLog): Boolean = true

  implicit val configs: Configs[NoConf] = Configs.fromTry((_, _) => NoConf)
}