package com.digischool.kkp.dsl2

import com.digischool.kkp.dsl2.models.path._
import com.digischool.kkp.dsl2.routedsl.PathParser
import org.parboiled2.ParserInput
import org.scalatest.{TestSuite, WordSpecLike}


class PathSpec extends TestSuite with WordSpecLike {


  "The PathParser" should {

    "parse Regex Path" in {
      val testString = """ <^.*>  """
      parsePath(testString, RegexPath(RegexPathPart("^.*".r)))
    }

    "parse INVALID Regex Path" in {
      val testString = """ <*>  """
      failToParsePath(testString)
    }

    "parse / Path" in {
      val testString = "/"
      parsePath(testString, SegmentPath(List(MultiPartSegment(Nil))))
    }

    "parse /toto Path" in {
      val testString = "/toto"
      parsePath(testString, SegmentPath(List(MultiPartSegment(List(PathPartString("toto"))))))
    }

    "parse /toto/ Path" in {
      val testString = "/toto/"
      parsePath(testString, SegmentPath(List(
        MultiPartSegment(List(PathPartString("toto"))),
        MultiPartSegment(Nil)
      )))
    }

    "parse /toto/titi Path" in {
      val testString = """ /toto/titi """
      parsePath(testString, SegmentPath(List(MultiPartSegment(List(PathPartString("toto"))), MultiPartSegment(List(PathPartString("titi"))))))
    }

    """parse explicit Path("/toto/titi")""" in {
      parsePath("""Path("/toto/titi")""", SegmentPath(List(MultiPartSegment(List(PathPartString("toto"))), MultiPartSegment(List(PathPartString("titi"))))), explicit = true)
    }

    "parse /<0-9+>/ Path" in {
      val testString = """ /<0-9+>/ """
      parsePath(testString, SegmentPath(List(
        MultiPartSegment(List(RegexPathPart("0-9+".r))),
        MultiPartSegment(Nil)
      )))
    }

    "parse /toto/<0-9+> Path" in {
      val testString = """ /toto/<0-9+> """
      parsePath(testString, SegmentPath(List(MultiPartSegment(List(PathPartString("toto"))), MultiPartSegment(List(RegexPathPart("0-9+".r))))))
    }

    "parse /toto/titi<0-9+> Path" in {
      val testString = """ /toto/titi<0-9+> """
      parsePath(testString, SegmentPath(List(MultiPartSegment(List(PathPartString("toto"))), MultiPartSegment(List(PathPartString("titi"), RegexPathPart("0-9+".r))))))
    }

    "parse /titi<UUID>toto<DOUBLE>tutu<INT><LONG><HEXINT><HEXLONG><ANY>/ Path" in {
      val testString = """ /titi<UUID>toto<DOUBLE>tutu<INT><LONG><HEXINT><HEXLONG><ANY>/ """
      parsePath(testString, SegmentPath(List(
        MultiPartSegment(List(PathPartString("titi"), UUID, PathPartString("toto"), DOUBLE, PathPartString("tutu"), INT, LONG, HEXINT, HEXLONG, ANY)),
        MultiPartSegment(Nil)
      )))
    }

    "parse /<END> Path" in {
      val testString = "/<END>"
      parsePath(testString, SegmentPath(List(MultiPartSegment(List(END)))))
    }


    "parse /toto<END> Path" in {
      val testString = "/toto<END>"
      parsePath(testString, SegmentPath(List(MultiPartSegment(List(PathPartString("toto"), END)))))
    }

    "parse SHOULD NOT ALLOW ANYTHING BUT SPACES AFTER <END> /toto<END>titi Path" in {
      val testString = "/toto<END>titi"
      failToParsePath(testString)
    }

  }

  def parsePath(s: String, testValue: Path, explicit: Boolean = false) = {
    val parser = new PathParser{
      val input = ParserInput(s)
      val explicitPath = explicit
    }
    parseAndCompare(parser, parser.testPath.run(), testValue)
  }

  def failToParsePath(s: String) = {
    val parser = new PathParser {
      override val explicitPath: Boolean = true
      override val input: ParserInput = s
    }
    failToParse(parser, parser.testPath.run())
  }

}
