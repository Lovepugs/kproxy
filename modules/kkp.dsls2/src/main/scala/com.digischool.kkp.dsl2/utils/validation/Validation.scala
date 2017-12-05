package com.digischool.kkp.dsl2.utils.validation

sealed trait Validation[+T] {
  def get: T
  def mapErrors(f: List[String] => List[String]): Validation[T]

  def mapError(f: String => String): Validation[T] = mapErrors(_.map(f))
  def groupErrors(title: String) = mapErrors(errs => title :: errs.map("\t|" + _))
}

object Validation {
  def success[T](t: T) = Valid(t)
  def error[T](err: String): Validation[T] = Invalid(err.split("\n").toList)
  def errors[T](errs: String*): Validation[T] = Invalid(errs.toList.flatMap(_.split("\n")))
}

case class Valid[+T](get: T) extends Validation[T] {
  override def mapErrors(f: (List[String]) => List[String]): Validation[T] = this
}

case class Invalid(errs: List[String]) extends Validation[Nothing] {
  override def get: Nothing = throw new Exception(errs.mkString("Invalid values:\n\t", "\n\t", ""))

  override def mapErrors(f: (List[String]) => List[String]): Validation[Nothing] = Validation.errors(f(errs): _*)
}
