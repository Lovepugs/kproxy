package com.digischool.kkp.keycloak.models.headers

import com.digischool.kkp.keycloak.models.JsonSpec
import org.scalatest.EitherValues
import play.api.libs.json.Format

import scala.reflect.ClassTag

/**
  * Created by cyrille on 27/05/2016.
  */
trait HeaderSpec[T] extends JsonSpec[T] with EitherValues {

  def headerDescriber(examples: Examples)(implicit C: ClassTag[T], F: Format[T], H: Header[T]) = s"A $name header" should {

    examples.foreach{ case (exName, (example, json)) =>
        val headerValue = H.encode(json.as[T])

        s"be decoded as valid when previously encoded for $exName" in {
          H.decode(H.encode(example)).right.value shouldBe example
        }

        s"be encoded as valid when previously decoded for $exName" in {
          H.decode(headerValue).right.map(H.encode).right.value shouldBe headerValue
        }
    }
  }


}
