package com.digischool.kkp.utils

import java.nio.charset.Charset
import java.util.Base64

import play.api.libs.json.{JsValue, Json, Writes}

import scala.util.Try

/**
  * Created by cyrille on 12/05/2016.
  */
private[kkp] object JsonBase64 {
  private val charset = Charset.forName("UTF-8")

  def encode[T: Writes](js: T) = Base64.getEncoder.encodeToString(Json.toJson(js).toString.getBytes(charset))

  def decode(encoded: String): Either[String, JsValue] = for {
    decoded <- Try(Base64.getDecoder.decode(encoded.getBytes(charset))).toOption.toRight("invalid Base64 bits").right
    json <- Try(Json.parse(decoded)).toOption.toRight("invalid json").right
  } yield json
}