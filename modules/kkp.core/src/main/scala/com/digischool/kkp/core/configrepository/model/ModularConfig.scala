package com.digischool.kkp.core.configrepository.model

import com.typesafe.config.Config

/**
  * The model for kProxyConfig.
  * @param confFiles the list of the configs, indexed by fileName
  *                  (should be full path, otherwise, there might be some configs erased by the Map structure)
  * @param modules  the list of modules given as their fully qualified class name
  */
case class ModularConfig(confFiles: Map[String, Config], modules: List[String])
