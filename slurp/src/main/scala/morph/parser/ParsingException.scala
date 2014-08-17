package morph.parser

/**
 * An exception that parsers throw if they encounter an error while parsing.
 *
 * @author Anish Athalye
 */
class ParsingException(message: String = null, cause: Throwable = null)
  extends RuntimeException(message, cause)

object ParsingException {

  def apply() = new ParsingException(null, null)

  def apply(msg: String) = new ParsingException(msg, null)

  def apply(msg: String, cause: Throwable) = new ParsingException(msg, cause)

  def apply(cause: Throwable) = new ParsingException(null, cause)
}
