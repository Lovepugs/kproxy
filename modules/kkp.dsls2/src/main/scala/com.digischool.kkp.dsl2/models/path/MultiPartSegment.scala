package com.digischool.kkp.dsl2.models.path

case class MultiPartSegment(parts: List[PathPartType])

object MultiPartSegment {
  def apply(parts: PathPartType*): MultiPartSegment = MultiPartSegment(parts.toList)
}
