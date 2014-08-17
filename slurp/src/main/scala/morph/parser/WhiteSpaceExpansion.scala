package morph.parser

import scala.language.implicitConversions

import org.parboiled.scala._

/**
 * A mixin for Parsers that makes matching whitespace easier.
 *
 * When this trait is mixed in, all strings of the form `"text "`
 * (note the trailing space) are automatically converted to a
 * rule that matches all trailing whitespace characters as well.
 * Whitespace characters are defined as any of:
 * `' '`, `'\n'`, `'\r'`, `'\t'`, `'\f'`, but this behavior can
 * be overridden.
 *
 * @author Anish Athalye
 */
trait WhiteSpaceExpansion {

  // self type, class mixing this in must be a Parser
  this: Parser =>

  /**
   * Rule that matches all whitespace.
   *
   * Matches all whitespace including `' '` (space), `'\n'` (newline),
   * `'\r'` (carriage return), `'\t'` (tab), and `'\f'` (form feed).
   */
  def WhiteSpace: Rule0 = rule { zeroOrMore(anyOf(" \n\r\t\f")) }

  /**
   * An implicit conversion to make writing whitespace matching rules
   * less tedious.
   *
   * When converting a string to a rule, if the string ends with a space,
   * this automatically converts it to a rule that matches any whitespace
   * after the string.
   */
  override implicit def toRule(string: String) = {
    if (string.endsWith(" ")) {
      str(string.trim) ~ WhiteSpace
    } else {
      str(string)
    }
  }
}
