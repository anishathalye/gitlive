package morph.extractor

import morph.ast._

/**
 * The base class that all extractors should extend.
 *
 * This class mixes in the DSL and AST-related implicit conversions
 * so that subclasses can omit those imports.
 *
 * Subclasses must define the extract method (inherited from AstTransformer),
 * which takes a ValueNode and performs extractions / transformations on it.
 *
 * @author Anish Athalye
 */
abstract class Extractor extends AstTransformer with DSL with Implicits
