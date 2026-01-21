package tokenizer

/**
 * Exception thrown when tokenisation fails.
 *
 * @property position the character position in the source where the error occurred (0-indexed)
 * @property context a snippet of the source around the error position
 */
class TokenisationError(
    message: String,
    val position: Int,
    val context: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    override fun toString(): String = "TokenisationError at position $position: $message\n  Context: \"$context\""
}
