package com.digischool.kkp.dsl2

import com.digischool.kkp.dsl2.models.path.{MultiPartSegment, PathPartString, SegmentPath}
import com.digischool.kkp.dsl2.models.{GET, Methods, MultipartDirective, POST}
import com.digischool.kkp.dsl2.routedsl.DirectiveParser
import org.scalatest.{TestSuite, WordSpecLike}

/**
  * Created by cyrille on 22/03/2016.
  */
class DirectiveSpec extends TestSuite with WordSpecLike {

  "the DirectiveParser" should {
    "parse a simple directive with spaces" in {
    val testString = """POST /titi/tata  GET"""
      val parser = new DirectiveParser[Nothing](testString)
      parseAndCompare(parser, parser.OnlyDirective.run(), MultipartDirective(
        Methods(POST),
        SegmentPath(MultiPartSegment(PathPartString("titi")), MultiPartSegment(PathPartString("tata"))),
        Methods(GET)
      ))
    }
  }
}
