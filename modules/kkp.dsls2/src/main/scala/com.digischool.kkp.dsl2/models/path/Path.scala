package com.digischool.kkp.dsl2.models.path

import com.digischool.kkp.dsl2.models.Directive
import com.digischool.kkp.dsl2.routedsl.PathParser
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.digischool.kkp.dsl2.routedsl.generic.rules.MyRule
import org.parboiled2.{ParserInput, Rule1}

sealed trait Path extends Directive[Nothing]

object Path {
  implicit val pathParseable = new Parseable[Path] {
    override val name: String = "Path"

    override def typeParser(input: ParserInput): MyRule[Path] = new MyRule[Path](input) with PathParser {
      override val explicitPath: Boolean = false

      override def myRule: Rule1[Path] = rule { '"' ~ Path  ~ '"'}
    }
  }
}

case class SegmentPath(segments:List[MultiPartSegment]) extends Path

object SegmentPath {
  def apply(segments: MultiPartSegment*): SegmentPath = SegmentPath(segments.toList)
}

case class RegexPath(r: RegexPathPart) extends Path{

  /**
    * val r1 = "^.*".r
    * val r2 = "^.*".r
    * r1.==(r2) // false !!!!
    *
    * @param obj: Any
    * @return Boolean equality
    */
  override def equals(obj:Any): Boolean = {
    obj.isInstanceOf[RegexPath] && obj.asInstanceOf[RegexPath].r.toString == this.r.toString}
}

