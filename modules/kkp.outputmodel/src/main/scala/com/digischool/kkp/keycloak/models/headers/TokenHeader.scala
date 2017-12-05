package com.digischool.kkp.keycloak.models.headers
import com.kreactive.capsule.StringValueClass

/**
  * Created by cyrille on 24/05/2016.
  */
case class TokenHeader(token: String) extends AnyVal

case object TokenHeader extends StringValueClass[TokenHeader] {
  override def construct: (String) => TokenHeader = apply
  override def deconstruct: (TokenHeader) => String = _.token

  implicit val header: Header[TokenHeader] =
    Header.asString[TokenHeader]("KPROXY-TOKEN")
}