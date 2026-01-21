package tokenizer

import lexer.Verilex

/**
 * Base class for tokenisers.
 *
 * @param T the type of tokens produced by this tokeniser
 */
abstract class Tokeniser<T> {
    /**
     * Tokenise the given source string.
     *
     * @param source the input string to tokenise
     * @return a sequence of tokens
     * @throws TokenisationError if the input cannot be tokenised
     */
    abstract fun tokenise(source: String): T

    protected fun buildTokenList(rawTokens: List<Pair<String, String>>): List<Token> {
        var position = 0
        return rawTokens.map { (tag, lexeme) ->
            Token.fromPair(tag to lexeme, position).also {
                position += lexeme.length
            }
        }
    }

    /**
     * Create a [TokenisationError] from a Verilex failure.
     */
    protected fun createLexicalError(
        source: String,
        cause: IllegalStateException,
    ): TokenisationError {
        val position = findFailurePosition(source)
        val context = extractContext(source, position)
        return TokenisationError(
            message = "Unexpected character at position $position",
            position = position,
            context = context,
            cause = cause,
        )
    }

    /**
     * Find the position where tokenisation failed (binary search).
     */
    protected fun findFailurePosition(source: String): Int {
        var low = 0
        var high = source.length

        while (low < high) {
            val mid = low + (high - low) / 2
            val prefix = source.substring(0, mid)
            if (canTokenise(prefix)) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low.coerceIn(0, source.length - 1)
    }

    /**
     * Check if a string can be successfully tokenised.
     */
    protected fun canTokenise(source: String): Boolean {
        if (source.isEmpty()) return true
        return try {
            Verilex.lex(SMLLexerSpec.lexer, source)
            true
        } catch (_: IllegalStateException) {
            false
        }
    }

    /**
     * Extract a context snippet around a position for error reporting.
     */
    protected fun extractContext(
        source: String,
        position: Int,
        windowSize: Int = 20,
    ): String {
        val start = (position - windowSize / 2).coerceAtLeast(0)
        val end = (position + windowSize / 2).coerceAtMost(source.length)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < source.length) "..." else ""
        return prefix + source.substring(start, end).replace("\n", "\\n") + suffix
    }
}
