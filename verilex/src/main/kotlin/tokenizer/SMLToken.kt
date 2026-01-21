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
        ABSTYPE("abstype"),
        AND("and"),
        ANDALSO("andalso"),
        AS("as"),
        CASE("case"),
        DATATYPE("datatype"),
        DO("do"),
        ELSE("else"),
        END("end"),
        EXCEPTION("exception"),
        FN("fn"),
        FUN("fun"),
        HANDLE("handle"),
        IF("if"),
        IN("in"),
        INFIX("infix"),
        INFIXR("infixr"),
        LET("let"),
        LOCAL("local"),
        NONFIX("nonfix"),
        OF("of"),
        OP("op"),
        OPEN("open"),
        ORELSE("orelse"),
        RAISE("raise"),
        REC("rec"),
        THEN("then"),
        TYPE("type"),
        VAL("val"),
        WITH("with"),
        WITHTYPE("withtype"),
        WHILE("while"),
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
        INTEGER("integer"),
        HEX_INTEGER("hex_integer"),
        WORD("word"),
        HEX_WORD("hex_word"),
        REAL("real"),
        STRING("string"),
        CHAR("char"),
    }

    /** Identifiers and type variables */
    enum class Identifier(
        override val displayName: String,
    ) : SMLTokenType {
        ALPHANUMERIC_ID("alphanumeric_id"),
        SYMBOLIC_ID("symbolic_id"),
        TYVAR("tyvar"),
        ETYVAR("etyvar"),
        NUMERIC_LABEL("numeric_label"),
    }

    /** Whitespace and comments */
    enum class Trivia(
        override val displayName: String,
    ) : SMLTokenType {
        WHITESPACE("whitespace"),
        NEWLINE("newline"),
        COMMENT("comment"),
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
    fun toCompactString(): String = "${type.displayName}($lexeme)"

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
    fun withoutTrivia(): List<Token> = tokens.filterNot { it.type is SMLTokenType.Trivia }

    /** Render as a compact string for testing */
    fun toCompactString(): String = tokens.joinToString(" ") { it.toCompactString() }

    /** Render as detailed multi-line string */
    fun toDetailedString(): String = tokens.joinToString("\n") { it.toString() }
}
