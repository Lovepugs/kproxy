package com.digischool.kkp.core.utils


import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.{ HttpCharsets, MediaTypes }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import play.api.libs.json.{ JsResultException, JsValue, Json, Reads, Writes }
import scala.language.implicitConversions

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *play-json* protocol.
  * https://github.com/hseeberger/akka-http-json/blob/master/akka-http-play-json/src/main/scala/de/heikoseeberger/akkahttpplayjson/PlayJsonSupport.scala
  */
object PlayJsonSupport extends PlayJsonSupport

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *play-json* protocol.
  */
trait PlayJsonSupport {

  implicit def playJsonUnmarshallerConverter[A](reads: Reads[A]): FromEntityUnmarshaller[A] =
    playJsonUnmarshaller(reads)

  implicit def playJsonUnmarshaller[A](implicit reads: Reads[A]): FromEntityUnmarshaller[A] = {
    def read(json: JsValue) = reads.reads(json).recoverTotal(error => throw JsResultException(error.errors))
    playJsValueUnmarshaller.map(read)
  }

  implicit def playJsValueUnmarshaller: FromEntityUnmarshaller[JsValue] =
    Unmarshaller
      .byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        Json.parse(input)
      }

  implicit def playJsonMarshallerConverter[A](writes: Writes[A])(implicit printer: JsValue => String = Json.prettyPrint): ToEntityMarshaller[A] =
    playJsonMarshaller[A](writes, printer)

  implicit def playJsonMarshaller[A](implicit writes: Writes[A], printer: JsValue => String = Json.prettyPrint): ToEntityMarshaller[A] =
    playJsValueMarshaller.compose(writes.writes)

  implicit def playJsValueMarshaller(implicit printer: JsValue => String = Json.prettyPrint): ToEntityMarshaller[JsValue] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`)(printer)
}