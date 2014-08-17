package morph.ast

import morph.ast.{ ValueNode => VN }

import scala.language.implicitConversions
import scala.{ PartialFunction => PF }

/**
 * A DSL for manipulating and transforming an AST.
 *
 * @author Anish Athalye
 */
trait DSL {

  /**
   * An implicit conversion from a type viewable as a `ValueNode` to
   * `Option[ValueNode]`.
   */
  implicit def ValueNodeViewable2OptionValueNode[T <% VN](
    nodeViewable: T): Option[VN] = Option(nodeViewable)

  /**
   * An implicit conversion from an option type with inner type viewable as
   * a `ValueNode` to `Option[ValueNode]`.
   */
  implicit def OptionValueNodeViewable2OptionValueNode[T <% VN](
    optNodeViewable: Option[T]): Option[VN] = optNodeViewable map { c => c: VN }

  /**
   * An implicit conversion from any type that can be viewed as a `ValueNode`
   * to an `RichOptionValueNode.
   *
   * This is necessary because Scala will never perform multiple implicit
   * conversions in a row. Without this, doing something like *(1) would not
   * work to construct an array containing a `NumberNode` because that would
   * require three implicit conversions `Int` to `ValueNode` to
   * `Option[ValueNode]` to `RichOptionValueNode`.
   *
   * Introducing another implicit conversion that can convert any type that is
   * viewable as a `ValueNode` into a `RichOptionValueNode` solves this
   * problem. This is done using a type parameter `T` that is viewable as a
   * `ValueNode` solves this problem.
   */
  implicit def ValueNodeViewable2RichOptionValueNode[T <% VN](
    nodeViewable: T): RichOptionValueNode = RichOptionValueNode(nodeViewable)

  /**
   * The implicit class that provides the majority of the methods in the DSL.
   *
   * These methods operate on `Option`s because methods may or may not return
   * results. By having these methods on `Option`s, it is very easy to chain
   * them and automatically filter out empty results in the end using the
   * ^(...) object constructor or the *(...) array constructor.
   */
  implicit class RichOptionValueNode(opt: Option[VN]) {

    /**
     * Returns the value corresponding to the specified key in an object node.
     *
     * @example
     * {{{
     * scala> val obj = ObjectNode("one" -> 1, "two" -> 2)
     * scala> val one = obj get "one"
     * one: Option[morph.ast.ValueNode] = Some(1)
     * }}}
     *
     * @param key The key corresponding to the value to retrieve.
     *
     * @return The value.
     */
    def get(key: String): Option[VN] = opt flatMap {
      case ObjectNode(fields) => fields get key
      case _                  => None
    }

    /**
     * Returns the value corresponding to the specified key in an ObjectNode.
     *
     * @example
     * {{{
     * scala> val obj = ObjectNode("one" -> 1, "two" -> 2)
     * scala> val one = obj ~> "one"
     * one: Option[morph.ast.ValueNode] = Some(1)
     * }}}
     *
     * @param key The key corresponding to the value to retrieve.
     *
     * @return The value.
     */
    def ~>(key: String): Option[VN] = opt get key

    /**
     * Returns the element corresponding to the specified index in an array
     * node.
     *
     * @example
     * {{{
     * scala> val arr = ArrayNode("zero", "one", "two", "three")
     * scala> val first = arr get 1
     * first: Option[morph.ast.ValueNode] = Some("one")
     * }}}
     *
     * @note This method uses 0-based indexing.
     *
     * @param index The index of the element to retrieve.
     *
     * @return The element.
     */
    def get(index: Int): Option[VN] = opt flatMap {
      case ArrayNode(elem) => elem lift index
      case _               => None
    }

    /**
     * Returns the element corresponding to the specified index in an array
     * node.
     *
     * @example
     * {{{
     * scala> val arr = ArrayNode("one", "two", "three")
     * scala> val first = arr ~> 1
     * first: Option[morph.ast.ValueNode] = Some("one")
     * }}}
     *
     * @note This method uses 1-based indexing.
     *
     * @param index The index of the element to retrieve.
     *
     * @return The element.
     */
    def ~>(index: Int): Option[VN] = opt get index

    /**
     * Recursively searches for a value corresponding to the specified key.
     *
     * @example
     *
     * {{{
     * scala> val inner = ObjectNode("test" -> 1, "more" -> 2)
     * scala> val arr = ArrayNode(inner, ObjectNode("test" -> 2, "inner" -> inner))
     * scala> val test = arr recGet "test"
     * one: Option[morph.ast.ValueNode] =
     * Some([
     *   1,
     *   2,
     *   1
     * ])
     * }}}
     *
     * @note This method does a depth first traversal and is '''not'''
     * tail recursive.
     *
     * @param key The key corresponding to the values to retrieve.
     *
     * @return A list of all matching nodes.
     */
    def recGet(key: String): Option[ArrayNode] = {
      def iter(node: VN, key: String): IndexedSeq[VN] = node match {
        case ObjectNode(fields) => {
          val sub = fields.toVector map {
            case (k, v) => iter(v, key)
          }
          fields get key match {
            case Some(value) => value +: sub.flatten
            case None        => sub.flatten
          }
        }
        case ArrayNode(elem) => {
          val sub = elem map { iter(_, key) }
          sub.flatten
        }
        case _ => Vector()
      }
      opt map { node => ArrayNode(iter(node, key)) }
    }

    /**
     * Recursively searches for a value corresponding to the specified key.
     *
     * @example
     *
     * {{{
     * scala> val inner = ObjectNode("test" -> 1, "more" -> 2)
     * scala> val arr = ArrayNode(inner, ObjectNode("test" -> 2, "inner" -> inner))
     * scala> val test = arr ~>> "test"
     * one: Option[morph.ast.ValueNode] =
     * Some([
     *   1,
     *   2,
     *   1
     * ])
     * }}}
     *
     * @note This method does a depth first traversal and is '''not'''
     * tail recursive.
     *
     * @param key The key corresponding to the values to retrieve.
     *
     * @return A list of all matching nodes.
     */
    def ~>>(key: String): Option[ArrayNode] = opt recGet key

    /**
     * Maps a function over an array node.
     *
     * The function can return an `Option[ValueNode]`, or it can return
     * a `ValueNode` in which case the return value will be implicitly
     * converted to an `Option[ValueNode]`.
     *
     * @param func The function to map.
     *
     * @return An array with the function applied to each element.
     */
    def mapFunc(func: VN => Option[VN]): Option[VN] =
      opt mapPartial Function.unlift(func)

    /**
     * Maps a function over an array node.
     *
     * The function can return an `Option[ValueNode]`, or it can return
     * a `ValueNode` in which case the return value will be implicitly
     * converted to an `Option[ValueNode]`.
     *
     * @param func The function to map.
     *
     * @return An array with the function applied to each element.
     */
    def %->(func: VN => Option[VN]): Option[VN] = opt mapFunc func

    /**
     * Maps a partial function over an array node.
     *
     * @param func The partial function to map.
     *
     * @return An array with the function applied (where applicable)
     * to each element.
     */
    def mapPartial(func: PF[VN, VN]): Option[VN] = opt collect {
      case ArrayNode(elem) => ArrayNode(elem collect func)
    }

    /**
     * Maps a partial function over an array node.
     *
     * @param func The partial function to map.
     *
     * @return An array with the function applied (where applicable)
     * to each element.
     */
    def %~>(func: PF[VN, VN]): Option[VN] = opt mapPartial func

    /**
     * Applies a function to a value node or maps the function over the
     * elements of an array node.
     *
     * This is useful for dealing with ambiguities between a single element
     * and an array of elements.
     *
     * @note This method should not be used when there is an ambiguity
     * between an array of arrays and a single array.
     *
     * @param func The function to apply or map.
     *
     * @return Either an object with the function applied or an array
     * with the function applied to each element.
     */
    def applyOrMapFunc(func: VN => Option[VN]): Option[VN] = opt flatMap {
      case arr: ArrayNode => arr mapFunc func
      case other          => func(other)
    }

    /**
     * Applies a function to a value node or maps the function over the
     * elements of an array node.
     *
     * This is useful for dealing with ambiguities between a single element
     * and an array of elements.
     *
     * @note This method should not be used when there is an ambiguity
     * between an array of arrays and a single array.
     *
     * @param func The function to apply or map.
     *
     * @return Either an object with the function applied or an array
     * with the function applied to each element.
     */
    def %%->(func: VN => Option[VN]): Option[VN] = opt applyOrMapFunc func

    /**
     * Applies a partial function to a value node or maps the partial
     * function over the elements of an array node.
     *
     * This is useful for dealing with ambiguities between a single element
     * and an array of elements.
     *
     * @note This method should not be used when there is an ambiguity
     * between an array of arrays and a single array.
     *
     * @param func The partial function to apply or map.
     *
     * @return Either an object with the function applied (if applicable)
     * or an array with the function (where applicable) applied to each element.
     */
    def applyOrMapPartial(func: PF[VN, VN]): Option[VN] = opt flatMap {
      case arr: ArrayNode => arr mapPartial func
      case other          => func.lift(other)
    }

    /**
     * Applies a partial function to a value node or maps the partial
     * function over the elements of an array node.
     *
     * This is useful for dealing with ambiguities between a single element
     * and an array of elements.
     *
     * @note This method should not be used when there is an ambiguity
     * between an array of arrays and a single array.
     *
     * @param func The partial function to apply or map.
     *
     * @return Either an object with the function applied (if applicable)
     * or an array with the function (where applicable) applied to each element.
     */
    def %%~>(func: PF[VN, VN]): Option[VN] =
      opt applyOrMapPartial func

    /**
     * Filters an array node by a predicate.
     *
     * @example
     * {{{
     * scala> val arr = ArrayNode(1, 2, 3, 4, 5, 6, 7, "eight")
     * scala> val even = arr applyFilter {
     *      |   case NumberNode(value) if value % 2 == 0 => true
     *      |   case _ => false
     *      | }
     * even: Option[morph.ast.ValueNode] =
     * Some([
     *   2,
     *   4,
     *   6
     * ])
     * }}}
     *
     * @param pred The predicate to filter by.
     *
     * @return An array containing only the nodes that satisfy the predicate.
     */
    def applyFilter(pred: VN => Boolean): Option[VN] = opt collect {
      case ArrayNode(elem) => ArrayNode(elem filter pred)
    }

    /**
     * Encapsulates a node in an array if the node is not already an array.
     *
     * @example
     * {{{
     * scala> val arr = (1).encapsulate
     * arr: Option[morph.ast.ValueNode] =
     * Some([
     *   1
     * ])
     * }}}
     *
     * @return An array containing the single node, or the node itself if it
     * was already an array.
     */
    def encapsulate: Option[VN] = opt map {
      case arr: ArrayNode => arr
      case other          => ArrayNode(other)
    }

    /**
     * Flattens an array of arrays.
     *
     * @example
     * {{{
     * scala> val arr = ArrayNode(ArrayNode(1, 2), ArrayNode(3), ArrayNode())
     * scala> val flattened = arr.applyFlatten
     * flattened: Option[morph.ast.ValueNode] =
     * Some([
     *   1,
     *   2,
     *   3
     * ])
     * }}}
     *
     * @return An array that contains all the elements contained within inner
     * arrays.
     */
    def applyFlatten: Option[VN] = opt collect {
      case ArrayNode(elem) => ArrayNode(elem flatMap {
        case ArrayNode(inner) => inner
        case _                => List()
      })
    }

    /**
     * Flattens an array of elements that may or may not be arrays.
     *
     * If the node is a single element (that is not an array), the node
     * is returned as is.
     *
     * @example
     * {{{
     * scala> val arr = ArrayNode(ArrayNode(1, 2), 3)
     * scala> val flattened = arr.autoFlatten
     * flattened: Option[morph.ast.ValueNode] =
     * Some([
     *   1,
     *   2,
     *   3
     * ])
     * }}}
     *
     * @return An array that contains all the elements contained within inner
     * arrays (flattened if the inner element is an array).
     */
    def autoFlatten: Option[VN] = opt map {
      case ArrayNode(elem) => ArrayNode(elem flatMap {
        case ArrayNode(inner) => inner
        case other            => List(other)
      })
      case other => other
    }

    /**
     * Recursively flattens arrays (without recursively examining objects).
     *
     * If the node is a single element (that is not an array), the node
     * is returned as is.
     *
     * @example
     * {{{
     * scala> val arr = ArrayNode(ArrayNode(1, ArrayNode(2)), ArrayNode(3))
     * scala> val flattened = arr.autoFlatten
     * flattened: Option[morph.ast.ValueNode] =
     * Some([
     *   1,
     *   2,
     *   3
     * ])
     * }}}
     *
     * @return An array that contains all the elements contained within inner
     * elements (that are possibly arrays).
     */
    def recFlatten: Option[VN] = opt map {
      case ArrayNode(elem) => ArrayNode(elem flatMap {
        case arr: ArrayNode => arr.recFlatten.asVector
        case other          => List(other)
      })
      case other => other
    }

    /**
     * Returns true if and only if the node is empty.
     *
     * This is defined differently for the different node types. An `Option`
     * with the value `None` is considered to be empty. An `ObjectNode` with
     * no key/value pairs is empty. An `ArrayNode` with no elements is empty.
     * A `StringNode` with an empty string is empty. `BooleanNode`s are
     * nonempty, and `NullNode` is empty.
     *
     * @return `true` if the node is empty.
     */
    def nodeEmpty: Boolean = opt map {
      case ObjectNode(fields) => fields.isEmpty
      case ArrayNode(elem)    => elem.isEmpty
      case StringNode(s)      => s.isEmpty
      case NullNode           => true
      case _                  => false
    } getOrElse true

    /**
     * Returns true if the node is nonempty.
     *
     * This is defined differently for the different node types. An `Option`
     * with the value `None` is considered to be empty. An `ObjectNode` with
     * no key/value pairs is empty. An `ArrayNode` with no elements is empty.
     * A `StringNode` with an empty string is empty. `BooleanNode`s are
     * nonempty, and `NullNode` is empty.
     *
     * @return `true` if the node is not empty.
     */
    def nodeNonEmpty: Boolean = !nodeEmpty

    // Unsafe operations

    /**
     * Returns true if the node is an `ObjectNode`.
     *
     * @return `true` if the node is an `ObjectNode`.
     */
    def isObject: Boolean = opt map {
      _.isInstanceOf[ObjectNode]
    } getOrElse false

    /**
     * Converts the node to an `ObjectNode`.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not an `ObjectNode`.
     *
     * @return The node as an `ObjectNode`.
     */
    def asObjectNode: ObjectNode = opt map {
      case obj: ObjectNode => obj
      case _ => throw NodeExtractionException(
        "node is not an ObjectNode"
      )
    } getOrElse {
      throw NodeExtractionException("node is empty")
    }

    /**
     * Converts the node to an `ObjectNode` and extracts its fields.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not an `ObjectNode`.
     *
     * @return The node as a `Map[String, ValueNode]`.
     */
    def asMap: Map[String, VN] = asObjectNode.fields
    /**
     * Returns true if the node is an `ArrayNode`.
     *
     * @return `true` if the node is an `ObjectNode`.
     */
    def isArray: Boolean = opt map {
      _.isInstanceOf[ArrayNode]
    } getOrElse false

    /**
     * Converts the node to an `ArrayNode`.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not an `ArrayNode`.
     *
     * @return The node as an `ArrayNode`.
     */
    def asArrayNode: ArrayNode = opt map {
      case arr: ArrayNode => arr
      case _ => throw NodeExtractionException(
        "node is not an ArrayNode"
      )
    } getOrElse {
      throw NodeExtractionException("node is empty")
    }

    /**
     * Converts the node to an `ArrayNode` and extracts its elements.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not an `ArrayNode`.
     *
     * @return The node as a `IndexedSeq[ValueNode]`.
     */
    def asIndexedSeq: IndexedSeq[VN] = asArrayNode.elements
    /**
     * Converts the node to an `ArrayNode` and extracts its elements.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not an `ArrayNode`.
     *
     * @return The node as a `Vector[ValueNode]`.
     */
    def asVector: Vector[VN] = asIndexedSeq.toVector
    /**
     * Converts the node to an `ArrayNode` and extracts its elements.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not an `ArrayNode`.
     *
     * @return The node as a `List[ValueNode]`.
     */
    def asList: List[VN] = asVector.toList
    /**
     * Returns true if the node is a `StringNode`.
     *
     * @return `true` if the node is a `StringNode`.
     */
    def isString: Boolean = opt map {
      _.isInstanceOf[StringNode]
    } getOrElse false

    /**
     * Converts the node to a `StringNode`.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `StringNode`.
     *
     * @return The node as an `ArrayNode`.
     */
    def asStringNode: StringNode = opt map {
      case sn: StringNode => sn
      case _ => throw NodeExtractionException(
        "node is not a StringNode"
      )
    } getOrElse {
      throw NodeExtractionException("node is empty")
    }

    /**
     * Converts the node to a `StringNode` and extracts the string it contains.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `StringNode`.
     *
     * @return The node as a `String`.
     */
    def asString: String = opt map {
      case StringNode(str) => str
      case _ => throw NodeExtractionException(
        "node is not a StringNode"
      )
    } getOrElse {
      throw NodeExtractionException("node is empty")
    }

    /**
     * Returns true if the node is a `NumberNode`.
     *
     * @return `true` if the node is a `NumberNode`.
     */
    def isNumber: Boolean = opt map {
      _.isInstanceOf[NumberNode]
    } getOrElse false

    /**
     * Converts the node to a `NumberNode`.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `NumberNode`.
     *
     * @return The node as a `NumberNode`.
     */
    def asNumberNode: NumberNode = opt map {
      case nn: NumberNode => nn
      case _ => throw NodeExtractionException(
        "node is not a NumberNode"
      )
    } getOrElse {
      throw NodeExtractionException("node is empty")
    }

    /**
     * Converts the node to a `NumberNode` and extracts its value.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `NumberNode`.
     *
     * @return The node as a `BigDecimal`.
     */
    def asNumber: BigDecimal = asNumberNode.value

    /**
     * Converts the node to a `NumberNode` and extracts its value.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `NumberNode`.
     *
     * @return The node as a `BigDecimal`.
     */
    def asBigDecimal: BigDecimal = asNumber

    /**
     * Returns true if the node is a `BooleanNode`.
     *
     * @return `true` if the node is a `BooleanNode`.
     */
    def isBoolean: Boolean = opt map {
      _.isInstanceOf[BooleanNode]
    } getOrElse false

    /**
     * Converts the node to a `BooleanNode`.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `BooleanNode`.
     *
     * @return The node as a `BooleanNode`.
     */
    def asBooleanNode: BooleanNode = opt map {
      case bn: BooleanNode => bn
      case _ => throw NodeExtractionException(
        "node is not a BooleanNode"
      )
    } getOrElse {
      throw new NodeExtractionException("node is empty")
    }

    /**
     * Converts the node to a `BooleanNode` and extracts its value.
     *
     * @throws NodeExtractionException If the node is empty or the node
     * is not a `BooleanNode`.
     *
     * @return The node as a `Boolean`.
     */
    def asBoolean: Boolean = asBooleanNode.value
  }

  /**
   * An object that easily constructs `ObjectNode`s.
   *
   * This can construct `ObjectNode`s from `(String, Option[ValueNode])*`.
   * Empty values (`None`s) are automatically filtered out. This makes
   * it very easy to chain computations and then filter out the results
   * of unsuccessful ones.
   *
   * This constructor provides very concise syntax and flexibility,
   * and it is completely type safe.
   *
   * @example
   * {{{
   * scala> val obj = ^("one" -> 1, "true" -> true,
   *      |             "false" -> FalseNode, "null" -> NullNode,
   *      |             "test" -> "yes", "opt" -> Option(3))
   * obj: morph.ast.ObjectNode =
   * {
   *   "one": 1,
   *   "true": true,
   *   "false": false,
   *   "null": null,
   *   "test": "yes",
   *   "opt": 3
   * }
   * }}}
   *
   */
  object ^ {

    /**
     * Constructs an `ObjectNode`.
     *
     * @param fields The mappings to use to create an `ObjectNode`.
     *
     * @return An `ObjectNode` with the given mappings.
     */
    def apply(fields: (String, Option[VN])*): ObjectNode = {
      val flattened = fields collect { case (k, Some(v)) => k -> v }
      ObjectNode(flattened: _*)
    }
  }

  /**
   * An object that easily constructs `ArrayNode`s.
   *
   * This can construct `ArrayNode`s from `Option[ValueNode]*`. Empty
   * elements (`None`s) are automatically filtered out. This makes
   * it very easy to chain computations and then filter out the results
   * of unsuccessful ones.
   *
   * This constructor provides very concise syntax and flexibility,
   * and it is completely type safe.
   *
   * @example
   * {{{
   * scala> *(1, "two", Option(3), None, true, ^("test" -> true))
   * res3: morph.ast.ArrayNode =
   * [
   *   1,
   *   "two",
   *   3,
   *   true,
   *   {
   *     "test": true
   *   }
   * ]
   * }}}
   */
  object * {

    /**
     * Constructs an `ArrayNode`.
     *
     * @param elements The elements to use to create an `ArrayNode`.
     *
     * @return An `ArrayNode` with the given elements.
     */
    def apply(elements: Option[VN]*): ArrayNode =
      ArrayNode(elements.flatten: _*)
  }

  /**
   * Safely compute by catching `NodeExtractionException`
   * and returning None if that exception occurs.
   *
   * This method can be used like a block to safely perform computations
   * on values contained within `Option[ValueNode]`s. This method catches
   * `NodeExtractionException`s caused by the `as[type]` methods and returns
   * `None` if an exception is thrown. If a computation is successful, the
   * method returns the result wrapped in an `Option`.
   *
   * @example
   * {{{
   * scala> val arr = *(1, "two", 3, "four", 5, true)
   * scala> val doubled = arr mapFunc { elem =>
   *      |   Safely {
   *      |     (elem.asNumber * 2).toString + "!"
   *      |   }
   *      | }
   * doubled: Option[morph.ast.ValueNode] =
   * Some([
   *   "2!",
   *   "6!",
   *   "10!"
   * ])
   * }}}
   *
   * @param computation The computation to perform.
   *
   * @return The result of the computation wrapped in an `Option`
   * (if successful, otherwise `None`).
   */
  def Safely[T <% Option[VN]](computation: => T): Option[VN] = {
    try {
      computation
    } catch {
      case e: NodeExtractionException => None
    }
  }

  /**
   * An exception that is thrown when there is an error extracting a node.
   *
   * This may occur when attempting to get a `ValueNode` from an
   * `Option[ValueNode]` when it is empty.
   *
   * This may also occur when illegally converting a `ValueNode` to one
   * of its subtypes.
   */
  class NodeExtractionException(
    message: String = null,
    cause: Throwable = null)
      extends RuntimeException(message, cause)

  object NodeExtractionException {

    def apply() = new NodeExtractionException(null, null)

    def apply(msg: String) = new NodeExtractionException(msg, null)

    def apply(msg: String, cause: Throwable) =
      new NodeExtractionException(msg, cause)

    def apply(cause: Throwable) = new NodeExtractionException(null, cause)
  }
}

/**
 * A companion object that extends `DSL` so that DSL methods
 * may be imported into scope instead of using the DSL as a mixin.
 */
object DSL extends DSL
