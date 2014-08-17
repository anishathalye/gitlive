package morph.extractor

import morph.ast._

import io.Source

/**
 * An interface to make using extractors easy.
 *
 * This interface makes calling extractors look like part of
 * the DSL.
 *
 * @example
 * {{{
 * scala> val transformed = transform node dataNode using DataNodeExtractor
 * transformed: morph.ast.ValueNode = ...
 * }}}
 *
 * @author Anish Athalye
 */
object Interface {

  /**
   * An object to help transform various data sources.
   */
  object transform {

    /**
     * Transform a node.
     *
     * @param node The node to transform.
     *
     * @return A Transformable instance that can be transformed using an Extractor.
     */
    def node(node: ValueNode): Transformable = new Transformable(node)
  }

  /**
   * A class that holds ready-to-transform data.
   */
  class Transformable(node: ValueNode) {

    /**
     * Transform data using a specific extractor.
     *
     * @param extractor The extractor to use.
     *
     * @return The root of the transformed AST.
     */
    def using(extractor: AstTransformer): ValueNode = extractor(node)
  }
}
