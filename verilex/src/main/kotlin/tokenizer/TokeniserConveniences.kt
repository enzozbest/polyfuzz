package tokenizer

/**
 * Tokenize this string as SML source code.
 *
 * @return a [TokenSequence] containing all tokens
 * @throws TokenisationError if the source contains unrecognized characters
 */
fun String.tokenizeSML(): TokenSequence = SMLTokeniser.tokenise(this)

/**
 * Tokenize this string as SML source code, returning null on failure.
 *
 * @return a [TokenSequence] containing all tokens, or null if tokenization fails
 */
fun String.tokenizeSMLOrNull(): TokenSequence? = runCatching { tokenizeSML() }.getOrNull()
