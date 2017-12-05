package com.digischool.kkp.dsl2

import com.digischool.kkp.dsl2.exception._
import com.digischool.kkp.dsl2.routedsl.generic.Parseable
import com.digischool.kkp.dsl2.routedsl.{CaseClassParser, DirectiveParser}
import org.scalatest.{TestSuite, WordSpecLike}

import scala.util.Try


/**
  * Created by cyrille on 21/03/2016.
  */
class FunctionParserSpec extends TestSuite with WordSpecLike {

  "The FunctionParser" should {
    "parse a function without parameters or parentheses" in {
      val testString = """Logged"""
      parseFunction(testString, Logged())
    }

    "parse a function without parameters" in {
      val testString = """Logged()"""
      parseFunction(testString, Logged())
    }

    "parse a function with unnamed parameters" in {
      val testString = """Logged([toto,titi,tata])"""
      parseFunction(testString, Logged(List("toto", "titi", "tata")))
    }

    "parse a function with dot notation parameters" in {
      val testString = """Logged(["realm.toto", myapp.titi, tata])"""
      parseFunction(testString, Logged(List("realm.toto", "myapp.titi", "tata")))
    }

    "parse a function with named parameter" in {
      val testString = """Logged(roles=[toto,titi,tata])"""
      parseFunction(testString, Logged(List("toto", "titi", "tata")))
    }
    "parse a function with spaces everywhere" in {
      val testString = """Logged   (  [ toto,  titi   ,   tata ] )   """
      parseFunction(testString, Logged(List("toto", "titi", "tata")))
    }
    "fail to parse a function with too many parameters" in {
      val testString = """Logged   (  [ toto,  titi   ,   tata ] , 80)   """
      failFunction(testString, ParsingException.TOO_MANY_PARAMETERS, 39)
    }


    "parse a function with several parameters using default value" in {
      val testString = """Backend   (   192.168.0.1 , 80 )   """
      parseFunction(testString, Backend("192.168.0.1", 80))
    }
    "parse a function with all named parameters" in {
      val testString = """Backend   (  host =  192.168.0.1 , port = 80 , path = "/")   """
      parseFunction(testString, Backend(host = "192.168.0.1", port = 80, path = "/"))
    }
    "parse a function with some misplaced named parameters" in {
      val testString = """Backend   ( 192.168.0.1 ,  path = "/hello" , port = 8080)   """
      parseFunction(testString, Backend("192.168.0.1", path = "/hello", port = 8080))
    }
    "parse a function with some misplaced named parameter and some missing parameter" in {
      val testString = """Backend   ( 192.168.0.1 ,  path = "/hello")   """
      parseFunction(testString, Backend("192.168.0.1", path = "/hello"))
    }
    "parse a function with some very misplaced named parameter" in {
      val testString = """Backend   (  path = "/" , port = 80 , host =  192.168.0.1)   """
      parseFunction(testString, Backend(path = "/", port = 80, host = "192.168.0.1"))
    }
    "fail to parse a function with some unnamed parameter after a named parameter" in {
      val testString = """Backend   ( 192.168.0.1 ,  path = "/hello", 8080)   """
      failFunction(testString, ParsingException.UNNAMED_BEFORE_NAMED, 44)
    }
    "fail to parse a function with some parameter defined twice" in {
      val testString = """Backend   ( 192.168.0.1 ,  path = "/hello", path = "/world", port = 8080)   """
      failFunction(testString, ParsingException.TWICE_DEFINED_PARAMETER, 44)

    }
    "fail to parse a function with some parameter defined twice, second time being at the end" in {
      val testString = """Backend   ( 192.168.0.1 ,  path = "/hello", port = 8080, path = "/world")   """
      failFunction(testString, ParsingException.TWICE_DEFINED_PARAMETER, 57)
    }

    "fail to parse a function with some unknown parameter" in {
      val testString = """Backend   ( 192.168.0.1 ,  hello = "/hello", port = 8080, path = "/world")   """
      failFunction(testString, ParsingException.INVALID_PARAMETER, 27)
    }

    "fail to parse a function with some parameter defined twice, first time being unnamed" in {
      val testString = """Backend   ( 192.168.0.1 ,  path = "/hello", port = 8080, host = "/world")   """
      failFunction(testString, ParsingException.TWICE_DEFINED_PARAMETER, 57)
    }

    "fail to parse a function starting with a comma" in {
      val testString = """Backend   ( ,192.168.0.1 ,  path = "/hello", port = 8080)   """
      failFunction(testString, ParsingException.NO_COMMA_AT_START, 12)
    }

    "fail to parse a function with unnamed wrong type parameter" in {
      val testString = """Logged("toto")"""
      failFunction(testString, ParsingException.expectType(implicitly[Parseable[List[String]]].name), 7)
    }

    "fail to parse a function with unnamed wrong type parameter, not at start" in {
      val testString = """Backend(localhost, [toto])"""
      failFunction(testString, ParsingException.expectType(implicitly[Parseable[Int]].name), 19)
    }

    "fail to parse a function with named wrong type parameter" in {
      val testString = """Logged(roles = "toto")"""
      failFunction(testString, ParsingException.expectType(implicitly[Parseable[List[String]]].name), 15)
    }

    "fail to parse a function with named wrong type parameter, not at start" in {
      val testString = """Backend(hello, port = "toto")"""
      failFunction(testString, ParsingException.expectType(implicitly[Parseable[Int]].name), 22)
    }

    "parse several functions" in {
      val testString =
        """Backend   (  host =  192.168.0.1 , port = 80 , path = "/")
          |Backend   (  path = "/" , port = 80 , host =  192.168.0.1)   """.stripMargin
      parseFunctions(testString, List(
        Backend(host = "192.168.0.1", port = 80, path = "/"),
        Backend(path = "/", port = 80, host = "192.168.0.1")
      ))
    }
  }

  val functions: Map[String, CaseClassParser[Testable]] =
    Map("Logged" -> Logged.parser, "Backend" -> Backend.parser)

  def failFunction(str: String, e: String, index: Int) = {
    val parser = DirectiveParser(str, functions)
    failToParse(parser, Try(parser.TestNamed.run()).flatten.map(_.t), e, index)
  }

  def parseFunction(str: String, testValue: Testable) = {
    val parser = DirectiveParser(str, functions)
    parseAndCompare(parser, parser.TestNamed.run().map(_.t), testValue)
  }

  def parseFunctions(str: String, testValue: List[Testable]) = {
    val parser = DirectiveParser(str, functions)
    parseAndCompare(parser, parser.TestSeveral.run().map(_.map(_.t)), testValue)
  }
}
