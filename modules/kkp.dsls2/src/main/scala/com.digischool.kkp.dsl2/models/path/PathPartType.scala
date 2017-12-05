package com.digischool.kkp.dsl2.models.path

/**
  * Created by cyrille on 23/03/2016.
  */
import scala.util.matching.Regex


sealed trait PathPartType
case object INT extends PathPartType
case object LONG extends PathPartType
case object DOUBLE extends PathPartType
case object UUID extends PathPartType
case object HEXINT extends PathPartType
case object HEXLONG extends PathPartType
case object ANY extends PathPartType
case object END extends PathPartType
case class PathPartString(s:String) extends PathPartType
case class RegexPathPart(s:Regex) extends PathPartType{

  /**
    * val r1 = "^.*".r
    * val r2 = "^.*".r
    * r1.==(r2) // false !!!!
    *
    * @param obj : Any
    * @return Boolean equality
    */
  override def equals(obj:Any): Boolean = {
    obj.isInstanceOf[RegexPathPart] && obj.asInstanceOf[RegexPathPart].s.toString() == this.s.toString()}
}




object PathPartType{
  def build(s:String): PathPartType = {
    s match {
      case "<INT>" => INT
      case "<LONG>" => LONG
      case "<DOUBLE>" => DOUBLE
      case "<UUID>" => UUID
      case "<HEXINT>" => HEXINT
      case "<HEXLONG>" => HEXLONG
      case "<ANY>" => ANY
      case "<END>" => END
      case _ => PathPartString(s)
    }
  }

  def buildRegexp(s:Regex) ={
    RegexPathPart(s)
  }
}
