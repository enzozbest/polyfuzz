package tokenizer

import lexer.Verilex

/**
 * Tokeniser for Standard ML source code.
 *
 * This tokeniser uses the Verilex derivative-based lexer engine with the SML token specification defined in [SMLLexerSpec].
 * It produces a [TokenSequence] of [Token] objects with full position tracking.
 * Important: the tokeniser follows POSIX lexing rules:
 *   1. Maximal munch (longest match) semantics
 *   2. Reserved word priority over identifiers
 */
object SMLTokeniser : Tokeniser<TokenSequence>() {
    /**
     * Tokenise an SML source string into a sequence of tokens.
     *
     * @param source the SML source code
     * @return a [TokenSequence] containing all tokens (including whitespace)
     * @throws TokenisationError if the source contains unrecognised characters
     */
    override fun tokenise(source: String): TokenSequence {
        if (source.isEmpty()) return TokenSequence(emptyList())

        return try {
            val rawTokens = Verilex.lex(SMLLexerSpec.lexer, source)
            val tokens = buildTokenList(rawTokens)
            TokenSequence(tokens)
        } catch (e: IllegalStateException) {
            throw createLexicalError(source, e)
        }
    }
}
