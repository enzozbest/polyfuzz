package lexer

import tokenizer.SMLTokenType
import tokenizer.SMLTokeniser
import tokenizer.TokenSequence
import tokenizer.TokenisationError

/**
 * In-process verilex invocation via SMLTokeniser.
 *
 * IMPORTANT: Calls withoutTrivia() before toCompactString() because polylex does not emit whitespace/newline/comment
 * tokens. Without filtering trivia, the verilex output would contain WS, NL, C tokens that polylex never produces,
 * making comparison impossible.
 */
object VerilexInvoker {
    /**
     * Tokenise SML source through verilex in-process.
     *
     * @param smlSource The SML source code to tokenise
     * @return LexerResult.Success with compact token string (trivia filtered),
     *         or LexerResult.Failure if tokenisation throws TokenisationError
     */
    fun invoke(smlSource: String): LexerResult =
        try {
            val allTokens = SMLTokeniser.tokenise(smlSource).withoutTrivia()
            val errorCount = allTokens.count { it.type is SMLTokenType.Error }
            val compact = TokenSequence(allTokens.filter { it.type !is SMLTokenType.Error }).toCompactString()
            if (compact.isBlank() && errorCount == 0) {
                LexerResult.Failure("verilex produced empty token stream")
            } else {
                LexerResult.Success(compact, errorCount)
            }
        } catch (e: TokenisationError) {
            LexerResult.Failure("verilex tokenisation failed at position ${e.position}: ${e.message}")
        } catch (e: Exception) {
            LexerResult.Failure("verilex invocation failed: ${e.message}")
        }
}
