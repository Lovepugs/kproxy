package com.digischool.kkp.keycloak.models

import org.scalatest.{Matchers, OptionValues, TestSuite, WordSpecLike}
import play.api.libs.json.{Format, JsValue, Json}

import scala.reflect.ClassTag

/**
  * Created by cyrille on 27/05/2016.
  */
trait JsonSpec[T] extends TestSuite with WordSpecLike with Matchers with OptionValues {

  type Examples = Map[String, (T, JsValue)]
  def name(implicit T: ClassTag[T]) = T.runtimeClass.getSimpleName

  def jsonDescriber(examples: Examples)(implicit T: Format[T], C: ClassTag[T]) = s"A $name JSON model" should {

    examples.foreach { case (exName, (example, json)) =>

      s"have a valid JSON representation for $exName" in {
        Json.toJson(example) === json
      }

      s"be readable from a valid JSON representation for $exName" in {
        json.asOpt[T].value shouldBe example
      }
    }
  }
}
