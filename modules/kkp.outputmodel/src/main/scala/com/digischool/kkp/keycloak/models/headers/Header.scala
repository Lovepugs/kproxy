package com.digischool.kkp.keycloak.models.headers

import com.digischool.kkp.utils.JsonBase64
import com.kreactive.util.Stringable
import play.api.libs.json._

/**
  * Created by cyrille on 12/05/2016.
  */
trait Header[T] {
  type Internal <: JsValue
  val header: String
  val oldHeader: String

  def headerNames = Set(header, oldHeader)

  def encode(o: T): String

  def setHeaderAs(t: T)(implicit T: Writes[T]): Map[String, String] =
    Map(header -> encode(t))

  protected def decodeInternal(s: String): Either[String, Internal]

  def decode(s: String)(implicit T: Reads[T]): Either[String, T] = for {
    json <- decodeInternal(s).right
    t <- json.asOpt[T].toRight(s"invalid model for header $header").right
  } yield t

  def getHeaderFrom(headers: Map[String, String])(implicit T: Reads[T]): Either[String, T] = for {
    h <- headers.get(header).toRight("invalid header").right
    t <- decode(h).right
  } yield t
}

object Header {
  def apply[T](implicit T: Header[T]): Header[T] = T

  def asJson[T: Writes](h: String, old:String): Header[T] = new Header[T] {
    override type Internal = JsValue

    override val header: String = h
    override val oldHeader: String = old

    override def encode(o: T): String = JsonBase64.encode(o)
    override protected def decodeInternal(s: String): Either[String, JsValue] = JsonBase64.decode(s)
  }

  def asJson[T: Writes](h: String): Header[T] = asJson[T](h, h)

  def asString[T](h: String, old: String)(implicit S: Stringable[T]): Header[T] = new Header[T] {
    override type Internal = JsString
    override val header: String = h
    override val oldHeader: String = old

    override def encode(o: T): String = S.asString(o)

    override protected def decodeInternal(s: String): Either[String, JsString] = Right(JsString(s))
  }

  def asString[T: Stringable](h: String): Header[T] = asString[T](h, h)

}