package tokenizer

/**
 * Represents a token type in Standard ML.
 */
sealed interface SMLTokenType {
    val displayName: String

    companion object {
        /**
         * Look up a token type by its tag in O(1).
         */
        private val byTagName: Map<String, SMLTokenType> by lazy {
            buildMap {
                ReservedWord.entries.forEach { put(it.displayName, it) }
                Punctuation.entries.forEach { put(it.displayName, it) }
                Literal.entries.forEach { put(it.displayName, it) }
                Identifier.entries.forEach { put(it.displayName, it) }
                Trivia.entries.forEach { put(it.displayName, it) }
            }
        }

        fun fromTag(tag: String): SMLTokenType = byTagName[tag] ?: error("Unknown token type: $tag")
    }

    /** Reserved words */
    enum class ReservedWord(
        override val displayName: String,
    ) : SMLTokenType {
        ABSTYPE("ABSTYPE"),
        AND("AND"),
        ANDALSO("ANDALSO"),
        AS("AS"),
        CASE("CASE"),
        DATATYPE("DATATYPE"),
        DO("DO"),
        ELSE("ELSE"),
        END("END"),
        EXCEPTION("EXCEPTION"),
        FN("FN"),
        FUN("FUN"),
        HANDLE("HANDLE"),
        IF("IF"),
        IN("IN"),
        INFIX("INFIX"),
        INFIXR("INFIXR"),
        LET("LET"),
        LOCAL("LOCAL"),
        NONFIX("NONFIX"),
        OF("OF"),
        OP("OP"),
        OPEN("OPEN"),
        ORELSE("ORELSE"),
        RAISE("RAISE"),
        REC("REC"),
        THEN("THEN"),
        TYPE("TYPE"),
        VAL("VAL"),
        WITH("WITH"),
        WITHTYPE("WITHTYPE"),
        WHILE("WHILE"),
        ;

        companion object {
            private val byName: Map<String, ReservedWord> = entries.associateBy { it.displayName }

            fun fromLexeme(lexeme: String): ReservedWord? = byName[lexeme]
        }
    }

    /** Punctuation*/
    enum class Punctuation(
        override val displayName: String,
    ) : SMLTokenType {
        LPAREN("("),
        RPAREN(")"),
        LBRACK("["),
        RBRACK("]"),
        LBRACE("{"),
        RBRACE("}"),
        COMMA(","),
        COLON(":"),
        SEMICOLON(";"),
        ELLIPSIS("..."),
        UNDERBAR("_"),
        PIPE("|"),
        EQUALS("="),
        DOUBLE_ARROW("=>"),
        ARROW("->"),
        HASH("#"),
    }

    /** Literal constants */
    enum class Literal(
        override val displayName: String,
    ) : SMLTokenType {
        INTEGER("INT"),
        WORD("WORD"),
        REAL("REAL"),
        STRING("STRING"),
        CHAR("CHAR"),
    }

    /** Identifiers and type variables */
    enum class Identifier(
        override val displayName: String,
    ) : SMLTokenType {
        IDENTIFIER("ID"),
        TYPE_IDENTIFIER("TYVAR"),
        NUMERIC_LABEL("numeric_label"),
    }

    /** Whitespace and comments */
    enum class Trivia(
        override val displayName: String,
    ) : SMLTokenType {
        WHITESPACE("WS"),
        NEWLINE("NL"),
        COMMENT("C"),
    }
}

/**
 * A token produced by the SML lexer.
 *
 * @property type The semantic type of this token
 * @property lexeme The actual text that was matched
 * @property position The starting position in the source (0-indexed)
 */
data class Token(
    val type: SMLTokenType,
    val lexeme: String,
    val position: Int,
) {
    /** Length of the token in characters */
    val length: Int get() = lexeme.length

    /** End position */
    val endPosition: Int get() = position + length

    override fun toString(): String = "Token($type, \"$lexeme\", pos=$position)"

    /**
     * Compact string representation suitable for testing and debugging.
     * Format: TYPE(lexeme)
     */
    fun toCompactString(): String {
        if (type is SMLTokenType.Literal || type is SMLTokenType.Identifier)
            return "${type.displayName}($lexeme)"
        return type.displayName
    }

    companion object {
        /**
         * Create a token from a tag-lexeme pair (as returned by Verilex).
         */
        fun fromPair(
            pair: Pair<String, String>,
            position: Int,
        ): Token = Token(SMLTokenType.fromTag(pair.first), pair.second, position)
    }
}

/**
 * Represents the result of tokenising a source string.
 * Provides convenient access patterns for the token sequence.
 */
@JvmInline
value class TokenSequence(
    private val tokens: List<Token>,
) : List<Token> by tokens {
    /** Get tokens without trivia (whitespace, comments, newlines) */
    fun withoutTrivia(): TokenSequence = TokenSequence(tokens.filterNot { it.type is SMLTokenType.Trivia })

    /** Render as a compact string for testing */
    fun toCompactString(): String = tokens.joinToString(" ") { it.toCompactString() }

    /** Render as detailed multi-line string */
    fun toDetailedString(): String = tokens.joinToString("\n") { it.toString() }
}
