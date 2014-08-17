package morph.parser

import morph.ast._

import org.parboiled.scala._
import org.parboiled.errors.ErrorUtils

/**
 * A base class for parsers.
 *
 * This class provides the boilerplate code that all Parboiled
 * parsers will need. Subclasses only need to provide the
 * parboiled rules for transforming input into an AST.
 *
 * @author Anish Athalye
 */
abstract class BaseParser extends Parser with AstBuilder {

  /**
   * The root parsing rule.
   *
   * This is the rule that will be run when there is input
   * to be parsed. It should return the root of the generated AST.
   */
  def RootRule: Rule1[ValueNode]

  /**
   * The main parsing method.
   *
   * If an error is encountered when parsing, this method does not
   * make any attempt to continue.
   *
   * @param input The content to parse.
   *
   * @throws ParsingException If the parser encounters an error while parsing.
   *
   * @return The root of the generated AST.
   */
  def apply(input: String): ValueNode = apply(input.toCharArray)

  /**
   * The main parsing method.
   *
   * If an error is encountered when parsing, this method does not
   * make any attempt to continue.
   *
   * @param input The content to parse.
   *
   * @throws ParsingException If the parser encounters an error while parsing.
   *
   * @return The root of the generated AST.
   */
  def apply(input: Array[Char]): ValueNode = {
    val parsingResult = ReportingParseRunner(RootRule).run(input)
    parsingResult.result getOrElse {
      throw ParsingException("Invalid source:\n" +
        ErrorUtils.printParseErrors(parsingResult))
    }
  }
}
