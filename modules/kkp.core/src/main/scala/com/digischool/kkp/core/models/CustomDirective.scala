package com.digischool.kkp.core.models

import java.util.NoSuchElementException

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive0, Directives}
import com.typesafe.config.Config
import play.api.libs.functional.Monoid

trait CustomDirective extends ((ActorSystem, Config) => Directive0) { self: Product =>
  //force case class toString
  override def toString = productIterator.mkString(productPrefix + "(", ", ", ")")
}

object CustomDirective {
  def apply(s: (ActorSystem, Config) => Directive0, name: String = "<customDirective>"): CustomDirective = new Product with CustomDirective {
    override def productElement(n: Int): Any = throw new NoSuchElementException(s"SingletonCustomDirective.productElement($n)")

    override def productArity: Int = 0

    override def apply(v1: ActorSystem, v2: Config): Directive0 = s(v1, v2)

    override def canEqual(that: Any): Boolean = false

    override def productPrefix: String = name
  }

  implicit val monoid: Monoid[CustomDirective] = new Monoid[CustomDirective] {
    override def append(a1: CustomDirective, a2: CustomDirective): CustomDirective = CustomDirective((sys, conf) => a1(sys, conf) & a2(sys, conf), s"$a1 & $a2")

    override def identity: CustomDirective = apply((_, _) => Directives.pass, "pass")
  }
}