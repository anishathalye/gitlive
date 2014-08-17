package morph.ast

import morph.utils.Utils._

import collection.immutable.ListMap
import scala.language.implicitConversions

/**
 * The abstract superclass for all AST node types.
 *
 * @author Anish Athalye
 */
sealed abstract class ValueNode

/**
 * An object.
 *
 * @author Anish Athalye
 */
case class ObjectNode(fields: Map[String, ValueNode]) extends ValueNode {

  override def toString = {
    val mapStr = fields map {
      case (k, v) => "\"" + k + "\": " + v
    } mkString ",\n"
    if (fields.isEmpty) {
      "{}"
    } else {
      "{\n" + mapStr.indent + "\n}"
    }
  }
}

object ObjectNode {

  def apply(fields: (String, ValueNode)*) =
    new ObjectNode(ListMap(fields: _*))

  def apply(fields: List[(String, ValueNode)]) =
    new ObjectNode(ListMap(fields: _*))
}

/**
 * An array.
 *
 * @author Anish Athalye
 */
case class ArrayNode(elements: IndexedSeq[ValueNode]) extends ValueNode {

  override def toString = if (elements.isEmpty) {
    "[]"
  } else {
    "[\n" + elements.mkString(",\n").indent + "\n]"
  }
}

object ArrayNode {

  def apply(elements: List[ValueNode]) = new ArrayNode(elements.toVector)

  def apply(elements: ValueNode*) = new ArrayNode(elements.toVector)
}

/**
 * A string.
 *
 * @author Anish Athalye
 */
case class StringNode(value: String) extends ValueNode {

  override def toString = "\"" + value.escaped + "\""
}

object StringNode {

  def apply(sym: Symbol) = new StringNode(sym.name)
}

/**
 * A number.
 *
 * A generic number type internally represented as a BigDecimal.
 *
 * @author Anish Athalye
 */
case class NumberNode(value: BigDecimal) extends ValueNode {

  override def toString = value.toString
}

object NumberNode {

  def apply(n: Int) = new NumberNode(BigDecimal(n))

  def apply(n: Long) = new NumberNode(BigDecimal(n))

  def apply(n: Double) = n match {
    case n if n.isNaN      => NullNode
    case n if n.isInfinity => NullNode
    case _                 => new NumberNode(BigDecimal(n))
  }

  def apply(n: BigInt) = new NumberNode(BigDecimal(n))

  def apply(n: String) = new NumberNode(BigDecimal(n))
}

sealed abstract class BooleanNode extends ValueNode {

  def value: Boolean

  override def toString = value.toString
}

/**
 * A boolean.
 *
 * @author Anish Athalye
 */
object BooleanNode {

  def apply(x: Boolean): BooleanNode =
    if (x) TrueNode else FalseNode

  def unapply(x: BooleanNode): Option[Boolean] = Some(x.value)
}

case object TrueNode extends BooleanNode {

  def value = true
}

case object FalseNode extends BooleanNode {

  def value = false
}

/**
 * A null value.
 *
 * @author Anish Athalye
 */
case object NullNode extends ValueNode {

  override def toString = "null"
}

/**
 * Implicit conversions from several scala built in data types to
 * their corresponding AST representations.
 *
 * @author Anish Athalye
 */
trait Implicits {

  implicit def String2StringNode(s: String): StringNode = StringNode(s)

  implicit def Boolean2BooleanNode(b: Boolean): BooleanNode = BooleanNode(b)

  implicit def Int2NumberNode(n: Int): NumberNode = NumberNode(n)

  implicit def Long2NumberNode(n: Long): NumberNode = NumberNode(n)

  implicit def Double2NumberNode(n: Double): NumberNode = NumberNode(n)

  implicit def BigDecimal2NumberNode(n: BigDecimal): NumberNode = NumberNode(n)

  implicit def StringValueNodeViewable2StringValueNode[T <% ValueNode](
    ss: (String, T)): (String, ValueNode) = (ss._1, ss._2)
}

/**
 * A companion object to make it possible to either
 * mix in the trait or import the companion object's methods.
 */
object Implicits extends Implicits
