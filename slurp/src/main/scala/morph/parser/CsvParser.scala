package morph.parser

import morph.ast._

import org.parboiled.scala._
import org.parboiled.Context

import scala.collection.mutable.Builder

/**
 * A CSV parser that constructs an AST.
 *
 * This parser can parse CSV files that conform to the RFC 4180 spec.
 * This parser was implemented almost directly from the ABNF grammar
 * found in the RFC. Note that input to this parser  must be valid
 * according to the spec to be able to be correctly parsed.
 *
 * @author Anish Athalye
 */
object CsvParser extends BaseParser {

  def RootRule = Csv

  lazy val Csv = rule { File ~ optional(CRLF) ~ EOI }

  def File = rule { FileUnwrapped ~~> { ArrayNode(_) } }

  def FileUnwrapped = rule { Records ~~> { _.result } }

  def Records = rule {
    push(Vector.newBuilder[ArrayNode]) ~ oneOrMore(rule {
      Record ~~% { withContext(appendToVb(_: ArrayNode, _)) }
    }, separator = CRLF)
  }

  def Record = rule { RecordUnwrapped ~~> { ArrayNode(_) } }

  def RecordUnwrapped = rule { Fields ~~> { _.result } }

  def Fields = rule {
    push(Vector.newBuilder[StringNode]) ~ oneOrMore(rule {
      Field ~~% { withContext(appendToVb(_: StringNode, _)) }
    }, separator = COMMA)
  }

  def Field = rule { Escaped | NonEscaped }

  def Escaped = rule {
    DQUOTE ~
      zeroOrMore(TEXTDATA | COMMA | CR | LF | DDQUOTE) ~>
      { s => StringNode(unDDQUOTE(s)) } ~
      DQUOTE
  }

  def NonEscaped = rule {
    zeroOrMore(TEXTDATA) ~> { s => StringNode(s) }
  }

  def unDDQUOTE(s: String) = s.replace(DDQUOTE, DQUOTE)

  def COMMA = ","

  def CR = "\r"

  def LF = "\n"

  def CRLF = rule { LF | CR + LF }

  def DQUOTE = "\""

  def DDQUOTE = "\"\""

  def TEXTDATA = rule { " " - "!" | "#" - "+" | "-" - "~" }

  def appendToVb[T](e: T, ctx: Context[_]) {
    ctx.getValueStack.peek.asInstanceOf[Builder[T, Vector[T]]] += e
  }
}
