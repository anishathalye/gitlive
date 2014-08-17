package morph.parser

import morph.ast._

import org.parboiled.scala._
import org.parboiled.Context

import scala.collection.mutable.Builder
import java.lang.StringBuilder

/**
 * A JSON parser that constructs an AST.
 *
 * It can parse JSON files that conform to the specification available
 * at www.json.org. This implimentation almost directly follows the
 * grammar specified there.
 *
 * @author Anish Athalye
 */
object JsonParser extends BaseParser with WhiteSpaceExpansion {

  def RootRule = Json

  lazy val Json = rule { WhiteSpace ~ Value ~ EOI }

  def JsonObject = rule {
    "{ " ~ JsonObjectUnwrapped ~ "} " ~~> { ObjectNode(_) }
  }

  def JsonObjectUnwrapped = rule { Pairs ~~> { _.result } }

  def Pairs = rule {
    push(Map.newBuilder[String, ValueNode]) ~ zeroOrMore(rule {
      Pair ~~% { withContext[String, ValueNode, Unit](appendToMb) }
    }, separator = ", ")
  }

  def Pair = rule { JsonStringUnwrapped ~ ": " ~ Value }

  def Value: Rule1[ValueNode] = rule {
    JsonString | JsonNumber | JsonObject | JsonArray |
      JsonTrue | JsonFalse | JsonNull
  }

  def JsonString = rule { JsonStringUnwrapped ~~> { StringNode(_) } }

  def JsonStringUnwrapped = rule {
    "\"" ~ Characters ~ "\" " ~~> { _.toString }
  }

  def JsonNumber = rule {
    group(Integer ~ optional(Frac) ~ optional(Exp)) ~>
      { NumberNode(_) } ~ WhiteSpace
  }

  def JsonArray = rule {
    "[ " ~ JsonArrayUnwrapped ~ "] " ~~> { ArrayNode(_) }
  }

  def JsonArrayUnwrapped = rule { Values ~~> { _.result } }

  def Values = rule {
    push(Vector.newBuilder[ValueNode]) ~ zeroOrMore(rule {
      Value ~~% { withContext(appendToVb(_: ValueNode, _)) }
    }, separator = ", ")
  }

  def Characters = rule {
    push(new StringBuilder) ~ zeroOrMore("\\" ~ EscapedChar | NormalChar)
  }

  def EscapedChar = {
    def unicode(code: Int, ctx: Context[_]) {
      appendToSb(code.asInstanceOf[Char], ctx)
    }
    def escaped(c: Char, ctx: Context[_]) {
      appendToSb('\\', ctx)
      appendToSb(c, ctx)
    }
    rule {
      anyOf("\"\\/") ~:% withContext(appendToSb) |
        anyOf("bfnrt") ~:% withContext(escaped) |
        Unicode ~~% {
          withContext(unicode)
        }
    }
  }

  def NormalChar = rule {
    !anyOf("\"\\") ~ ANY ~:% { withContext(appendToSb) }
  }

  def Unicode = rule {
    "u" ~ group(HexDigit ~ HexDigit ~ HexDigit ~ HexDigit) ~>
      { java.lang.Integer.parseInt(_, 16) }
  }

  def Integer = rule { optional("-") ~ (("1" - "9") ~ Digits | Digit) }

  def Digits = rule { oneOrMore(Digit) }

  def Digit = rule { "0" - "9" }

  def HexDigit = rule { Digit | "a" - "f" | "A" - "F" }

  def Frac = rule { "." ~ Digits }

  def Exp = rule { ignoreCase("e") ~ optional(anyOf("+-")) ~ Digits }

  def JsonTrue = rule { "true " ~ push(TrueNode) }

  def JsonFalse = rule { "false " ~ push(FalseNode) }

  def JsonNull = rule { "null " ~ push(NullNode) }

  def appendToSb(c: Char, ctx: Context[_]) {
    ctx.getValueStack.peek.asInstanceOf[StringBuilder].append(c)
  }

  def appendToMb(k: String, v: ValueNode, ctx: Context[_]) {
    ctx.getValueStack.peek.asInstanceOf[Builder[(String, ValueNode), Map[String, ValueNode]]] += ((k, v))
  }

  def appendToVb[T](e: T, ctx: Context[_]) {
    ctx.getValueStack.peek.asInstanceOf[Builder[T, Vector[T]]] += e
  }
}
