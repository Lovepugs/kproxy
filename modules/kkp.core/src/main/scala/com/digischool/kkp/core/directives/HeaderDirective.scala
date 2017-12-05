package com.digischool.kkp.core.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.BasicDirectives
import com.digischool.kkp.core.models.config.HeaderConfiguration

trait HeaderDirective extends BasicDirectives {

  /**
   * Enhance current headers with custom conf headers
   *
   * @param headers
   * @return
   */
  def enhanceHeaders (headers: Seq[HeaderConfiguration]): Directive0 = {

    val nHeaders: Seq[HttpHeader] =
      headers.map( h => HttpHeader.parse(h.name, h.value)) collect {
        case ParsingResult.Ok(x, _) => x
      }

    mapRequest{_.mapHeaders{headers =>
      headers.filterNot(x => nHeaders.exists(y => y.name() == x.name())) ++ nHeaders
    }}

  }

}

object HeaderDirective extends HeaderDirective