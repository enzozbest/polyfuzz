package lexer

/**
 * Uniform result type for both polylex and verilex invocations.
 *
 * Used by PolylexInvoker and VerilexInvoker to return token streams or error information to the comparison engine.
 */
sealed class LexerResult {
    data class Success(val tokenStream: String, val errorCount: Int = 0) : LexerResult()

    /**
     * Tokenisation failed. [error] describes what went wrong.
     *
     * For polylex: non-zero exit code or empty output. For verilex: TokenisationError exception.
     */
    data class Failure(val error: String) : LexerResult()
}
