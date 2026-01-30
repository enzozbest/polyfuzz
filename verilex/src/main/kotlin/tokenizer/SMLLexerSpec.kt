package tokenizer

import rexp.CHAR
import rexp.P
import rexp.RegularExpression
import rexp.S
import rexp.T
import rexp.X

/**
 * Combines all SML token patterns into a single tagged regular expression
 * suitable for use with the Verilex lexer.
 *
 * The order of alternation matters for disambiguation (a.k.a Rule Priority):
 * - Reserved words come before identifiers (so "if" matches as keyword, not identifier)
 * - Longer symbols come before shorter ones (so "=>" matches before "=")
 * - More specific patterns come before general ones (e.g. type variables before identifiers)
 */
object SMLLexerSpec {
    private val reservedWords: RegularExpression by lazy {
        ("ABSTYPE" T SMLReservedWords.abstype) X
            ("AND" T SMLReservedWords.and) X
            ("ANDALSO" T SMLReservedWords.andalso) X
            ("AS" T SMLReservedWords.as_) X
            ("CASE" T SMLReservedWords.case) X
            ("DATATYPE" T SMLReservedWords.datatype) X
            ("DO" T SMLReservedWords.do_) X
            ("ELSE" T SMLReservedWords.else_) X
            ("END" T SMLReservedWords.end) X
            ("EXCEPTION" T SMLReservedWords.exception) X
            ("FN" T SMLReservedWords.fn) X
            ("FUN" T SMLReservedWords.fun_) X
            ("HANDLE" T SMLReservedWords.handle) X
            ("IF" T SMLReservedWords.if_) X
            ("IN" T SMLReservedWords.in_) X
            ("INFIX" T SMLReservedWords.infix) X
            ("INFIXR" T SMLReservedWords.infixr) X
            ("LET" T SMLReservedWords.let) X
            ("LOCAL" T SMLReservedWords.local) X
            ("NONFIX" T SMLReservedWords.nonfix) X
            ("OF" T SMLReservedWords.of_) X
            ("OP" T SMLReservedWords.op_) X
            ("OPEN" T SMLReservedWords.open) X
            ("ORELSE" T SMLReservedWords.orelse) X
            ("RAISE" T SMLReservedWords.raise) X
            ("REC" T SMLReservedWords.rec) X
            ("THEN" T SMLReservedWords.then) X
            ("TYPE" T SMLReservedWords.type) X
            ("VAL" T SMLReservedWords.val_) X
            ("WITH" T SMLReservedWords.with) X
            ("WITHTYPE" T SMLReservedWords.withtype) X
            ("WHILE" T SMLReservedWords.while_)
    }

    private val punctuation =
        ("..." T SMLReservedWords.ellipsis) X
            ("=>" T SMLReservedWords.doubleArrow) X
            ("->" T SMLReservedWords.arrow) X
            ("(" T SMLReservedWords.lparen) X
            (")" T SMLReservedWords.rparen) X
            ("[" T SMLReservedWords.lbrack) X
            ("]" T SMLReservedWords.rbrack) X
            ("{" T SMLReservedWords.lbrace) X
            ("}" T SMLReservedWords.rbrace) X
            ("," T SMLReservedWords.comma) X
            (":" T SMLReservedWords.colon) X
            (";" T SMLReservedWords.semicolon) X
            ("_" T SMLReservedWords.underbar) X
            ("|" T SMLReservedWords.pipe) X
            ("=" T SMLReservedWords.equals) X
            ("#" T SMLReservedWords.hash)

    private val literals =
            ("WORD" T (SMLConstants.decimalWord X SMLConstants.hexWord)) X
            ("REAL" T SMLConstants.real) X
            ("INT" T (SMLConstants.decimalInteger X SMLConstants.hexInteger)) X
            ("STRING" T SMLStrings.string) X
            ("CHAR" T SMLStrings.char)

    private val identifiers =
            ("TYVAR" T SMLIdentifiers.tyvar) X
            ("numeric_label" T SMLIdentifiers.numericLabel) X // 1, 2, 3, ...
            ("ID" T (SMLIdentifiers.alphanumericId X SMLIdentifiers.qualifiedId X SMLIdentifiers.symbolicId))

    private val whitespace: RegularExpression = "WS" T SMLReservedWords.ws.P()
    private val newline: RegularExpression = "NL" T (CHAR('\n') X "\\r\\n").P()

    /**
     * The complete SML lexer specification as a single regular expression.
     *
     * Pattern priority (left-to-right in alternation):
     * 1. Reserved words (to avoid matching as identifiers)
     * 2. Punctuation (longer matches first)
     * 3. Literals (more specific patterns first)
     * 4. Identifiers (type variables before alphanumeric)
     * 5. Whitespace
     */
    val singleToken = reservedWords X punctuation X literals X identifiers X whitespace X newline

    /**
     * Lexer for a complete SML program
     */
    val lexer = singleToken.S().toCharFunctionFormat()
}
