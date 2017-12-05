package com.digischool.kkp.dsl2.utils

import com.digischool.kkp.dsl2.exception.ParsingException
import org.parboiled2.CharPredicate._
import org.parboiled2._
import shapeless.{::, HNil}


trait ParserUtils extends Parser {

  def getOrFail[T](msg: String): Rule[Option[T] :: HNil, T :: HNil] = rule { MATCH ~> ((opt: Option[T]) => FailIf(opt.isEmpty, msg) ~ push(opt.get)) }

  def FailIf(condition: Boolean, msg: String): Rule0 = rule { (test(condition) ~ fail(msg)) | MATCH}
  def FailIf(condition: Boolean, e: ParsingException): Rule0 = rule { (test(condition) ~ FailWith(e)) | MATCH}
  def FailWith(e: ParsingException): Rule0 = rule { fail(e.expected) }
  def NamedParams(name : String): Rule0 = rule { str(name) ~ Equals }
  def NamedParam: Rule1[String] = rule { Word ~ Equals }
  def NamedParamIn(names: Traversable[String]): Rule0 = rule { &(Word) ~ valueMap(names.map((_, ())).toMap) ~ Equals }

  // FIXME : this should be defined in a more graceful way
  def FilePath = rule { capture((FileNameSafeAlphabet | '/').*) }
  def Uri = rule { QuotedString | FileName }
  def FileName = rule { capture(FileNameSafeAlphabet.+) }
  def Word: Rule1[String] = rule{ capture((!Space ~ FileNameSafeAlphabet).+) ~ OptionalSpaces}
  def Words: Rule1[Seq[String]] = rule{ Word.*(OR) }
  def QuotedString: Rule1[String] = rule { TripleQuoted | Quoted }
  def Quoted: Rule1[String] = rule { '"' ~ InQuoteCharacter.* ~> ((l: Seq[String]) => l.mkString) ~ '"' ~ OptionalSpaces}
  def TripleQuoted: Rule1[String] = rule { TripleQuote ~ capture((!TripleQuote ~ ANY).*) ~ TripleQuote ~ capture(zeroOrMore('"')) ~ OptionalSpaces ~> ((a: String, b: String) => a + b )}
  def InQuoteCharacter: Rule1[String] = rule { capture(!'"' ~ !'\\' ~ ANY) | ( '\\' ~ capture(ANY))}
  def IntegralNumber = rule { capture(optional("+" | "-") ~ Digit.+) ~> ((x: String) => x.toInt) }
  def HexNumber = rule { "0x" ~ capture(HexDigit.+) ~> ((x: String) => Integer.parseInt(x, 16))}

  def OR: Rule0 = rule {OptionalSpaces ~ '|' ~ OptionalSpaces }
  def Equals: Rule0 = rule { OptionalSpaces ~ '=' ~ OptionalSpaces }
  def TripleQuote = rule { 3.times('"') }

  def LParen: Rule0 = rule { '(' ~ OptionalSpaces }
  def RParen: Rule0 = rule {OptionalSpaces ~ ')'}
  def LBrace: Rule0 = rule { '{' ~ OptionalSpaces }
  def RBrace: Rule0 = rule {OptionalSpaces ~ '}'}
  def LBracket: Rule0 = rule { "[ " }
  def RBracket: Rule0 = rule {OptionalSpaces ~ ']'}


  def FileNameSafeAlphabet: Rule0 = rule { AlphaNum | anyOf("-._") }
  def OptionalInlineSpaces: Rule0 = rule { quiet(InlineSpace.*) }
  def OptionalSpaces: Rule0 = rule { quiet(Space.*) }
  def Spaces: Rule0 = rule { quiet(Space.+) }
  def InlineSpaces: Rule0 = rule { quiet(InlineSpace.+) }
  def Space: Rule0 = rule { quiet(anyOf(" \n\r\t\f")) }
  def InlineSpace: Rule0 = rule { quiet(anyOf(" \r\t\f")) }
  def Comma: Rule0 = rule { quiet(OptionalSpaces ~ ',' ~ OptionalSpaces) }

  import scala.language.implicitConversions
  implicit def strToRule(baseStr: String): Rule0 =
    if (baseStr.endsWith(" ")) rule { str(baseStr.trim) ~ Space.* }
    else rule { str(baseStr) }

}
