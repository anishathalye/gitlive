package morph.extractor

import morph.ast._

/**
 * A trait describing methods that extractors must implement.
 *
 * The rest of the framework expects extractors to implement this trait.
 *
 * @author Anish Athalye
 */
trait AstTransformer {

  /**
   * The main extraction / transformation method.
   *
   * @param node The node to transform.
   *
   * @return A transformed node.
   */
  final def apply(node: ValueNode): ValueNode = extract(node)

  /**
   * The main extraction / transformation method.
   *
   * @param node The node to transform.
   *
   * @return A transformed node.
   */
  def extract(node: ValueNode): ValueNode
}
