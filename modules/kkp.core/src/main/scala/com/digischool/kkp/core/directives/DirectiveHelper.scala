package com.digischool.kkp.core.directives

import akka.http.scaladsl.server.{Directive, Directive0}

import scala.language.implicitConversions

/**
  * Created by cyrille on 03/05/2017.
  */
trait DirectiveHelper {
  // to avoid using `tflatMap` whenever we have Directive0
  implicit def directive0toDirective1(d: Directive0): Directive.SingleValueModifiers[Unit] =
    Directive.SingleValueModifiers(d.tmap(_ => Tuple1(())))

}
