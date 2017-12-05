package com.digischool.kkp.dsl2.routedsl

import com.digischool.kkp.dsl2.utils.ParserUtils
import org.parboiled2.CharPredicate._
import org.parboiled2.Rule1
import com.digischool.kkp.dsl2.models._, path._

import scala.util.Try


trait PathParser extends ParserUtils {
  val explicitPath: Boolean

  def testPath =  rule { OptionalSpaces ~ Path ~ OptionalSpaces ~ EOI }

  def Path: Rule1[Path] = rule {  (test(explicitPath) ~ ExplicitPath) | ImplicitPath}
  def ExplicitPath: Rule1[Path] = rule {"Path" ~ OptionalSpaces ~ LParen ~ '"' ~ ImplicitPath ~ '"' ~ RParen}
  def ImplicitPath: Rule1[Path] = rule { PathRegexp | PathSegments }

  def PathRegexp: Rule1[Path] = rule { Regexp  ~> RegexPath }

  def PathSegments: Rule1[Path] = rule {
    Slash ~ Segment.*(Slash) ~ End.? ~> ( (seq: Seq[MultiPartSegment], opt: Option[PathPartType]) => {
      val segments = opt.map { end =>
        val last: MultiPartSegment = seq.reverse.head
        val reversedTail = seq.reverse.tail
        val newLast = last.copy(parts = last.parts :+ end)
        (newLast :: reversedTail.toList).reverse
      }.getOrElse(seq.toList)
      SegmentPath(segments)
    })
  }

  def Segment: Rule1[MultiPartSegment] = rule { (SpecialTypes | Regexp | SegmentStringPart).* ~> { (seq: Seq[PathPartType]) => MultiPartSegment(seq.toList)} }

  def SegmentStringPart: Rule1[PathPartType] = rule { !(Slash | Spaces | "<") ~ capture(SegmentChars.+) ~> { (s: String) =>
    PathPartType.build(s)} }

  def SpecialTypes: Rule1[PathPartType] = rule {  !End ~ capture("<" ~ ("UUID" | "DOUBLE" | "INT" | "LONG" | "HEXINT" | "HEXLONG" | "ANY" ) ~ ">")  ~> ( (s: String) => PathPartType.build(s))  }

  def End: Rule1[PathPartType] = rule { capture("<END>") ~> ( (s:String) => PathPartType.build(s)) }

  def Regexp:Rule1[RegexPathPart] = rule{ !End ~ '<' ~ capture( (!">" ~ Printable).+) ~ '>' ~> {str:String =>
    val tryRegexp = Try(str.r)
    test(tryRegexp.isSuccess) ~ push(RegexPathPart(tryRegexp.get))
  }
  }

  def Slash =  rule { "/" }

  /** *
    * segment valid characters
    * see http://tools.ietf.org/html/rfc3986#appendix-A
    */
  def SegmentChars = rule {UnReserved | PercentEncoding | anyOf(SubDelim) | anyOf( ":@")}
  def PercentEncoding = rule { "%" ~ HexDigit ~ HexDigit }
  def UnReserved = rule { AlphaNum | anyOf("-._~") }
  val SubDelim = "!$&'()*+,;="


}

