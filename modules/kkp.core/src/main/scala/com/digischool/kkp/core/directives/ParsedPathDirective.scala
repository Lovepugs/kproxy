package com.digischool.kkp.core.directives

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.{Directives, PathMatcher, PathMatcher0, PathMatchers}
import com.digischool.kkp.dsl2.models.path._

import scala.language.implicitConversions
import scala.util.matching.Regex

object ParsedPathDirective {
  def directive(p: Path) = Directives.pathPrefix(pathMatcher(p))

  private def pathMatcher(p: Path):PathMatcher0   = {
    p match {
      case SegmentPath(segments: List[MultiPartSegment]) =>
        segments.map { segment =>
          segment.parts.map(p => p: PathMatcher0).foldLeft(PathMatcher(""))(_ ~ _)  //PathMatcher("") act as neutral element
        }.reduce(_ / _)
      case RegexPath(rpp@RegexPathPart(r: Regex)) => regexpPathMather(r)
    }
  }

  private def regexpPathMather(regex:Regex):PathMatcher0 = {
    new PathMatcher[Unit] {
      def apply(path: akka.http.scaladsl.model.Uri.Path) = {
        regex.findPrefixOf("/" + path.toString) match {
          case Some(m: String) ⇒ Matched(path.dropChars(m.length), ())
          case None ⇒ Unmatched
        }
      }
    }
  }

  implicit def PathPartType2PathMather1(pmt:PathPartType):PathMatcher0 = {
    pmt match {
      case INT => PathMatchers.IntNumber
      case LONG => PathMatchers.LongNumber
      case DOUBLE => PathMatchers.DoubleNumber
      case UUID => PathMatchers.JavaUUID
      case HEXINT => PathMatchers.HexIntNumber
      case HEXLONG => PathMatchers.HexLongNumber
      case END => PathMatchers.PathEnd
      case PathPartString(s) => s:PathMatcher0
      case RegexPathPart(r) => r: PathMatcher0
      case ANY => ".*".r : PathMatcher0
    }
  }

  // see akka-http issue #1010
  implicit def _regex2PathMatcher(regex: Regex): PathMatcher0 = new PathMatcher0 {
    def apply(path: Uri.Path) = path match {
      case Uri.Path.Segment(segment, tail) ⇒
        regex findPrefixOf segment match {
          case Some(m) ⇒
            Matched(segment.substring(m.length) :: tail, ())
          case None ⇒ Unmatched
        }
      case tail if regex.findFirstIn("").isDefined ⇒ Matched(tail, ())  // if segment is ended, check if empty string matches regex
      case _ ⇒ Unmatched
    }
  }

  implicit def PathMatcher1toPathMatcher0[L](underlying: PathMatcher[L]): PathMatcher0 =
    underlying.tmap(_ => ())
}
