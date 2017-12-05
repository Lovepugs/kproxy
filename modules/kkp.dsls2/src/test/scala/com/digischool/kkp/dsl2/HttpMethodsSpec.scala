package com.digischool.kkp.dsl2

import com.digischool.kkp.dsl2.models._
import com.digischool.kkp.dsl2.routedsl.DirectiveParser
import org.parboiled2.ParseError
import org.scalatest.{TestSuite, WordSpecLike}

import scala.util.{Failure, Success}



class HttpMethodsSpec extends TestSuite with WordSpecLike {


  "The HttpMethodsParser" should {
    "parse 'ALL http verbs' to List[String]" in {
      val testString = """GET|POST|PUT|PATCH|HEAD|DELETE|OPTIONS"""
      parseMethods(testString, Methods(List( GET, POST, PUT, PATCH, HEAD, DELETE, OPTIONS)))
    }

    "parse 'ALL http verbs with SPACES' to List[String]" in {
      val testString =  """    GET |   POST |PUT|HEAD|DELETE|OPTIONS|PATCH"""
      parseMethods(testString, Methods(List( GET, POST, PUT, HEAD, DELETE, OPTIONS, PATCH)))
    }

    "don't parse 'illegal http verb' to List[String]" in {
      val testString =  """ HELLO|  ALL  |  GET |   POST |PUT|HEAD|DELETE|OPTIONS|PATCH"""
      val parser = DirectiveParser[Nothing](testString)
      val result = parser.HttpMethods.run()
      result match {
        case Success(r) => ko("should not parse")
        case Failure(e: ParseError) => ok
        case Failure(_) => ko("should provide a ParseError")
      }
    }

    "don't Allow ALL with other Methods  (ALL  |  GET) " in {
      val testString =  """  ALL  |  GET |   POST |PUT|HEAD|DELETE|OPTIONS|PATCH"""
      val parser = DirectiveParser[Nothing](testString)
      val result = parser.HttpMethods.run()
      result match {
        case Success(r) => ko("sould not parse")
        case Failure(e: ParseError) => ok
        case Failure(_) => ko("should provide a ParseError")
      }
    }


    "don't Allow ALL with other Methods (GET | ALL |  POST)" in {
      val testString =  """   GET | ALL |  POST |PUT|HEAD|DELETE|OPTIONS|PATCH"""
      val parser = DirectiveParser[Nothing](testString)
      val result = parser.HttpMethods.run()
      result match {
        case Success(r) => ko("sould not parse")
        case Failure(e: ParseError) => ok
        case Failure(_) => ko("should provide a ParseError")
      }
    }

  }

  def parseMethods(s: String, testValue: MethodDirective) = {
    val parser = DirectiveParser[Nothing](s)
    parseAndCompare(parser, parser.testHttpMethods.run(), testValue)
  }

}
