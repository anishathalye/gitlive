package morph.parser

import morph.ast._

/**
 * A trait describing methods that AST builders must implement.
 *
 * The rest of the framework expects parsers to implement this trait,
 * so all custom parsers should implement this trait.
 *
 * @author Anish Athalye
 */
trait AstBuilder {

  /**
   * The main parsing method.
   *
   * @param input The content to parse.
   *
   * @return The root of the generated AST.
   */
  def apply(input: String): ValueNode

  /**
   * The main parsing method.
   *
   * @param input The content to parse.
   *
   * @return The root of the generated AST.
   */
  def apply(input: Array[Char]): ValueNode
}
