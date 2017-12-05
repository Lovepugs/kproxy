package com.digischool.kkp.dsl2.routedsl.generic

import com.digischool.kkp.dsl2.exception._
import com.digischool.kkp.dsl2.routedsl.generic.rules.{MyRule, MyTwoStageRule}
import org.parboiled2._
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, Witness}

/**
  * The type of the implicit carrying the logic for parsing a list of function parameters
  *
  * @tparam L the HList type of the list of parameters, labelled, as with LabelledGenerics
  */
trait HListParseable[L <: HList] {
  import HListParseable.Wrapped
  /**
    * An HList of `Option[_]` corresponding to `L`
    */
  type Options <: HList
  /**
    * The parser for a list of parameters
    * @param in the parser input
    * @param name the final name of the function (for error reporting)
    * @param stacked the values stacked up so far
    * @param missingPrevious the set of parameters which we have past through without parsing - for recursive calls
    * @param alreadySeen the set of parameters which we have already parsed - for recursive calls
    * @param onlyNamed a boolean to specify if unnamed parameter are still allowed - for recursive calls
    * @param atStart a boolean to specify if we are at start of the list of parameters we are parsing - for recursive calls
    */
  def recognizer(in: ParserInput,
                 name: String,
                 stacked: Option[Options] = None,
                 missingPrevious: Set[String] = Set.empty[String],
                 alreadySeen: Set[String] = Set.empty[String],
                 onlyNamed: Boolean = false,
                 atStart: Boolean = true): MyTwoStageRule[Set[String], Wrapped[Options]]

  /**
    * given an output, replace missing value by given defaults, and returns an error if a value is still missing
    * @param l the output of the parser
    * @param d the defaults values
    * @return either the list of missing parameters or the HList of parameters, either from `l` or `d`
    */
  def resolveDefaults[Defaults <: Options](l: Options, d: Option[Defaults]): Either[List[String], L]
  /**
    * Convenience method to convert the labels to a list of strings
    * @return the dynamic equivalent of the static list of labels
    */
  def parameters: List[String]

  /**
    * A static HList with only `None` values
    */
  def emptyOptions: Options

  /**
    * The parser for a list of parameters, with defaults value, and error if missing parameter
    * @param input the parser input
    * @param name the final name of the function (for error reporting)
    * @param d the default values to place where none were found
    * @throws MissingParametersException if some parameter has no parsed or default value
    */
  def recognizeWithDefaults(input: ParserInput, name: String, d: Option[Options] = None): MyRule[Wrapped[L]] = new MyRule[Wrapped[L]](input) {
    def rightOrFail(e: Either[List[String], L]): Rule1[Wrapped[L]] = rule {
      (test(e.isRight) | fail(ParsingException.MISSING_PARAMETER)) ~ push(Wrapped(e.right.get))
    }
    override def myRule: Rule1[Wrapped[L]] = rule {
      runSubParser(recognizer(_, name).myRule).named(s"$name parameters omitting defaults") ~> ((wl: Wrapped[Options]) => rightOrFail(resolveDefaults(wl.l, d)))
    }
  }
  /**
    * The name of the rule in the parser
    */
  lazy val name: String = parameters.mkString(", ")

}

object HListParseable {
  /**
    * Helper function to fetch implicit value in scope
    */
  def apply[L <: HList](implicit L: HListParseable[L]): Aux[L, L.Options] = L
  /**
    * The auxiliary type for HListParseable
    * @tparam L The Labelled HList we wish to parse
    * @tparam D correspond to L Mapped Option (without the labels)
    */
  type Aux[L <: HList, D <: HList] = HListParseable[L] {
    type Options = D
  }
  /**
    * To use an HList as a single element on the parboiled stack
    * @param l the HList
    * @tparam L its type
    */
  case class Wrapped[+L <: HList](l: L)

  /**
    * the HNil case (not much to say, here)
    */
  implicit val hnil: Aux[HNil, HNil] = new HListParseable[HNil] {
    override type Options = HNil
    override def recognizer(in: ParserInput,
                            name: String,
                            stacked: Option[Options] = None,
                            missingPrevious: Set[String] = Set.empty[String],
                            alreadySeen: Set[String] = Set.empty[String],
                            onlyNamed: Boolean = false,
                            atStart: Boolean = true): MyTwoStageRule[Set[String], Wrapped[Options]] = new MyTwoStageRule[Set[String], Wrapped[HNil]](in) {
      def myTempRule: TempRule = rule { push(alreadySeen) ~ push(Wrapped(stacked.getOrElse(HNil)))}
    }

    override def resolveDefaults[Defaults <: Options](l: Options, d: Option[Defaults]): Either[List[String], HNil] = Right(HNil)
    lazy val parameters: List[String] = Nil
    lazy val emptyOptions = HNil
  }

  /**
    * The HCons case, when the tail is HListParseable, the head is parseable and has a label
    * @param H the head type must be parseable
    * @param K the head label must be a symbol with a name
    * @param BoolH not null if the head type is `Boolean`
    * @param T the tail must be HListParseable
    * @tparam H the (unlabelled) type of the head of the HList
    * @tparam Key the label type of the head of the HList
    * @tparam T the type of the tail of the HList
    * @tparam OptionsTail the type of the tail of `Options`
    */
  implicit def hcons[H, Key <: Symbol, T <: HList, OptionsTail <: HList](implicit
                                                                   H: Parseable[H], K: Witness.Aux[Key], BoolH: H =:= Boolean = null,
                                                                   T: HListParseable.Aux[T, OptionsTail]): HListParseable.Aux[FieldType[Key, H] :: T, Option[H] :: OptionsTail] =
    new HListParseable[FieldType[Key, H] :: T] {
      override type Options = Option[H] :: OptionsTail
      val param = K.value.name
      lazy val emptyOptions: Options = None :: T.emptyOptions
      lazy val parameters = param :: T.parameters

      override def resolveDefaults[Defaults <: Option[H] :: OptionsTail](l: Option[H] :: OptionsTail, d: Option[Defaults]): Either[List[String], FieldType[Key, H] :: T] = {
        val defaults = d.getOrElse(emptyOptions)
        val head = l.head.orElse(defaults.head)
        val tail = T.resolveDefaults(l.tail, Some(defaults.tail))
        head.map(h => tail.right.map(h.asInstanceOf[FieldType[Key, H
          ]] :: _)).getOrElse(Left(param :: tail.left.getOrElse(Nil)))
      }

      /** The logic behind this is quite involved. See author if any question.
        * This implements the parsing of a list of parameters, separated by commas, to a HList of predefined type.
        * Values are parsed and put inside a HList of Option[_], which will be completed with default values afterward
        * It is done recursively on the size of the HList, with some interesting twists.
        *
        * We go through the parserInput, keeping track of the following variables (our parser allows stateless variable using subParsers):
        *  - stacked: the HList of parameters that have already been parsed, with None as placeholder
        *  - missingPrevious: the list of parameters which we expected but did not encountered. This allows us to recognize that we should backtrack our recursive calls
        *  - alreadySeen: the list of parameters we have already encountered (named or not), to avoid double definition of a parameter
        *  - onlyNamed: a boolean to forbid an unnamed parameter to appear after a named one
        *  - atStart: a convenience boolean to deal with some special cases that happen only for the first parameter
        *
        * Additionaly, if we are expecting at least one parameter (if the HList is not HNil), we know its name, its type T and how to parse it, using an implicit Parseable[T]
        *
        * Comma separators are parsed at the beginning of each step, except the first one (using atStart variable).
        * The trailing comma error is checked at the closing parenthesis stage, in the FunctionParser
        *
        * At each (recursive) call of the parser, we try in order the following cases:
        * - MissingPreviousNamedParam
        * - InplaceNamedParam
        * - MisplacedFirstParam
        * - InplaceUnnamedParam
        * - AbsentFirstParam
        */
      override def recognizer(in: ParserInput,
                              functionName: String,
                              stackedO: Option[Option[H] :: OptionsTail],
                              missingPrevious: Set[String] = Set.empty[String],
                              alreadySeen: Set[String] = Set.empty[String],
                              onlyNamed: Boolean = false,
                              atStart: Boolean = true) = new MyTwoStageRule[Set[String], Wrapped[Option[H] :: OptionsTail]](in) {
        lazy val stacked = stackedO.getOrElse(emptyOptions)
        val otherParams = T.parameters
        val WrongType: String =  ParsingException.expectType(H.name)
        val notBool = BoolH eq null
        /**
          * The input starts with `<previous_param> = ` with a missingPrevious parameter
          *     -> we consider that the function call is ended, and returns the list of already stacked values.
          *     This does not mean that the parsing ends, since the third case (MisplacedFirstParam) which populates the missingPrevious variable runs a secondary function call
          *     This does not move the cursor on the input, so that the secondary function call can pick-up were we left off.
          * @return the `alreadySeen` params (non updated, since we did not move the cursor) and the `stacked` values
          */
        def MissingPreviousNamedParam: TempRule = rule {
          &(MaybeComma ~ CheckValidPreviousNameParam) ~ AbsentFirstParam
        }
        //
        /**
          * the input starts with `<param> = ` with the first expected parameter name
          *     -> we try to parse its value using its type Parseable implicit value
          *     -> we add the parsed value to the head of the result of the recursive call, where `param` is `alreadySeen`
          * @throws TwiceDefinedParameterException if `param` was already seen
          * @throws WrongTypeParameterException if what follows `param` cannot be parsed as its expected type
          * @return the `alreadySeen` updated by the sub-instance and the `param`,
          *         the stacked values, with head the parsed value, and tail as returned by the sub-instance
          */
        def InplaceNamedParam: TempRule = rule {
          MaybeComma ~
            CheckNotAlreadySeenParam ~
            NamedParams(param) ~
            InPlaceNamedFirstParam ~
            push(missingPrevious) ~ push(alreadySeen + param) ~ push(onlyNamed || notBool) ~ push(false) ~ ParseSubInstance ~
            BindHeadValue
        }
        /**
          * the input starts with `<other_param> = `, with a name in the list of remaining parameters, but not in the list of already seen parameters.
          *     This means that the first expected param is missing for now.
          *     Since the recursion backtracks, it may happen that a parameter that we have already parsed is back in the list of remaining parameters (that's why we keep track of them).
          *     -> we stash the first expected param in missingPrevious, and we keep going. However, once this recursive call is ended,
          *        we retry to parse all parameter, starting with this stashed one, using the values we have already parsed as stacked,
          *        although keeping them as alreadySeen.
          *     This is the most complicated case. From another view point, when the parser stumbles upon a param it is not yet able to parse (because it knows only how to parse the first one),
          *       it will pause, go down its recursion to get to the point where it can parse it (this is what the sub-instance call do), then set its parsed value as stacked.
          *       After that, it comes back to parsing the first param, as if nothing happened
          * @return the `alreadySeen` updated by the sub-instance call (adding `other_param`) and the re-run,
          *         the stacked values, where `other_param` will be updated in place by the sub-instance call, and the others by the re-run
          */
        def MisplacedFirstParam: TempRule = rule {
          &(MaybeComma ~ CheckNotAlreadySeenParam ~ CheckKnownParam) ~
            push(missingPrevious + param) ~ push(alreadySeen) ~ push(false) ~ push(atStart) ~ ParseSubInstance ~>
            ((newAlreadySeen: Set[String], fi: Wrapped[OptionsTail]) =>
              // re-runs the parser with the values acquired from the previous call as default
              runSubParser(recognizer(_, functionName, Some(stacked.head :: fi.l), missingPrevious, newAlreadySeen, onlyNamed = true, atStart = false).myTempRule).named(s"$name without $param"))
        }
        /**
          * the input does not start with a named param
          *     -> we check if this is allowed (unnamed param cannot happen after named param, because we cannot know what they mean)
          *        we consider it is the first expected param, so parse it as such and then keep going (marking the first param as alreadySeen)
          *     There are some subtleties here: if the type parser mismatch, it is probably a WrongTypeException. However,
          *       if the function was called without any parameters, the type parser would mismatch on the closing parenthesis, so we need to treat this case specifically.
          * @return alreadySeen updated by `param` and the sub-instance call,
          *         the stacked values, with head updated by the parsed value, and others by the sub-instance call
          * @throws UnnamedBeforeNamedException if `onlyNamed` is true
          * @throws WrongTypeParameterException if the value cannot be parsed as the expected type
          */
        def InplaceUnnamedParam: TempRule = rule {
          MaybeComma ~
            FailIf(onlyNamed, ParsingException.UNNAMED_BEFORE_NAMED) ~
            InPlaceUnnamedFirstParam ~
            push(missingPrevious) ~ push(alreadySeen + param) ~ push(false) ~ push(false) ~ ParseSubInstance ~
            BindHeadValue
        }
        /**
          * (no comma check here): the input does not start with a named param or a recognizable param of the expected type
          *     -> we consider the function call ended
          * This can happen when stumbling upon a `missingPrevious`, in which case we are inside a `MisplacedFirstParam` sub-call,
          * or at the end of the call (notably if no comma is found)
          * @return the alreadySeen and the stackedValues
          */
        def AbsentFirstParam: TempRule = rule { push(alreadySeen) ~ push(Wrapped(stacked))}
        def myTempRule: TempRule =  rule {
          MissingPreviousNamedParam |
            InplaceNamedParam |
            MisplacedFirstParam |
            InplaceUnnamedParam |
            AbsentFirstParam
        }


        /** Helper rules **/
        def FirstParam: Rule1[H] = rule { runSubParser(i => H.typeParser(i).myRule).named(H.name) }
        // parse first param or fail with WrongTypeException (the push is never used (after a fail), and is there for type compatibility)
        def InPlaceNamedFirstParam = rule { FirstParam | (fail(WrongType) ~ push(stacked.head.get)) }
        // parse first param or fail with WrongTypeException (the push is never used (after a fail), and is there for type compatibility)
        def InPlaceUnnamedFirstParam = rule { FirstParam | (FailIf(!atStart, WrongType) ~ !RParen ~ fail(WrongType) ~ push(stacked.head.get)) }
        // expects a comma if not at start
        def MaybeComma: Rule0 = rule { (&(Comma) ~ FailIf(atStart, ParsingException.NO_COMMA_AT_START) ~ Comma) | test(atStart) }
        // expects a named param with a missing previous name
        def CheckValidPreviousNameParam: Rule0 = rule {&(NamedParamIn(missingPrevious))}
        // expects a named param with a not already seen param in the list of remaining params
        def CheckKnownParam: Rule0 = rule {
          &(NamedParamIn(otherParams)) | (&(NamedParam) ~ fail(ParsingException.INVALID_PARAMETER))
        }
        def CheckNotAlreadySeenParam = rule {
          (&(NamedParamIn(alreadySeen)) ~ fail(ParsingException.TWICE_DEFINED_PARAMETER)) | MATCH
        }
        // recursion call; must have missingPrevious, alreadySeen, onlyNamed and atStart on top of value stack
        def ParseSubInstance: Rule[Set[String] :: Set[String] :: Boolean :: Boolean :: HNil, Set[String] :: Wrapped[OptionsTail] :: HNil] = rule {
          MATCH ~> ((missing: Set[String], seen: Set[String], namedOnly: Boolean, isAtStart: Boolean) =>
            runSubParser(T.recognizer(_, functionName, Some(stacked.tail), missing, seen, onlyNamed = namedOnly, atStart = isAtStart).myTempRule).named("parameters tail"))
        }
        // binds a parameter value at the head of the partial FunctionInstance acquired from recursion
        def BindHeadValue: Rule[H :: Set[String] :: Wrapped[OptionsTail] :: HNil, Set[String] :: Wrapped[Option[H] :: OptionsTail] :: HNil] = rule {
          MATCH ~> ((h: H, newAlreadySeen: Set[String], fi: Wrapped[OptionsTail]) => newAlreadySeen :: Wrapped(Some(h) :: fi.l) :: HNil)
        }
      }
    }
}