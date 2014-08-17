package morph.utils

/**
 * A collection of various utilities.
 *
 * Most utilities are in the form of implicit classes contained
 * inside the `Utils` object.
 *
 * @author Anish Athalye
 */
object Utils {

  /**
   * An implicit class to provide additional methods on `String`.
   */
  implicit class StringUtils(val str: String) extends AnyVal {

    /**
     * Indent every line in the by two spaces.
     *
     * @note This works as expected with both `"\n"` and `"\r\n"` newlines.
     *
     * @return The indented string.
     */
    def indent: String = str indent 2

    /**
     * Indent every line in a string by a specified number of spaces.
     *
     * @note This works as expected with both `"\n"` and `"\r\n"` newlines.
     *
     * @param num The number of spaces to indent every line by.
     *
     * @return The indented string.
     */
    def indent(num: Int): String =
      (" " * num) + str.replace("\n", "\n" + " " * num)

    /**
     * Escape special characters with backslash escapes.
     *
     * This escapes the special charactesr in the string so that the string
     * will be suitable for being displayed as a double quoted string.
     *
     * The escape codes specified in JLS 3.10.6 are implemented, with the
     * exception of the single quote character (which does not need to be
     * escaped in a double quoted string).
     *
     * @example
     * {{{
     * scala> val str = """hello\
     *      | world"""
     * str: String =
     * hello\
     * world
     *
     * scala> val escaped = str.escaped
     * escaped: String = hello\\\nworld
     * }}}
     *
     * @return The string with escapes visibly escaped.
     */
    def escaped: String = str flatMap {
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case '\\' => "\\\\"
      case '"'  => "\\\""
      case c    => c.toString
    }
  }
}
