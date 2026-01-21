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
        ("abstype" T SMLReservedWords.abstype) X
            ("and" T SMLReservedWords.and) X
            ("andalso" T SMLReservedWords.andalso) X
            ("as" T SMLReservedWords.as_) X
            ("case" T SMLReservedWords.case) X
            ("datatype" T SMLReservedWords.datatype) X
            ("do" T SMLReservedWords.do_) X
            ("else" T SMLReservedWords.else_) X
            ("end" T SMLReservedWords.end) X
            ("exception" T SMLReservedWords.exception) X
            ("fn" T SMLReservedWords.fn) X
            ("fun" T SMLReservedWords.fun_) X
            ("handle" T SMLReservedWords.handle) X
            ("if" T SMLReservedWords.if_) X
            ("in" T SMLReservedWords.in_) X
            ("infix" T SMLReservedWords.infix) X
            ("infixr" T SMLReservedWords.infixr) X
            ("let" T SMLReservedWords.let) X
            ("local" T SMLReservedWords.local) X
            ("nonfix" T SMLReservedWords.nonfix) X
            ("of" T SMLReservedWords.of_) X
            ("op" T SMLReservedWords.op_) X
            ("open" T SMLReservedWords.open) X
            ("orelse" T SMLReservedWords.orelse) X
            ("raise" T SMLReservedWords.raise) X
            ("rec" T SMLReservedWords.rec) X
            ("then" T SMLReservedWords.then) X
            ("type" T SMLReservedWords.type) X
            ("val" T SMLReservedWords.val_) X
            ("with" T SMLReservedWords.with) X
            ("withtype" T SMLReservedWords.withtype) X
            ("while" T SMLReservedWords.while_)
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
        ("hex_word" T SMLConstants.hexWord) X
            ("word" T SMLConstants.decimalWord) X
            ("hex_integer" T SMLConstants.hexInteger) X
            ("real" T SMLConstants.real) X
            ("integer" T SMLConstants.decimalInteger) X
            ("string" T SMLStrings.string) X
            ("char" T SMLStrings.char)

    private val identifiers =
        ("etyvar" T SMLIdentifiers.etyvar) X // Must come before "tyvar"
            ("tyvar" T SMLIdentifiers.tyvar) X
            ("numeric_label" T SMLIdentifiers.numericLabel) X // 1, 2, 3, ...
            ("alphanumeric_id" T SMLIdentifiers.alphanumericId) X
            ("symbolic_id" T SMLIdentifiers.symbolicId)

    private val whitespace: RegularExpression = "whitespace" T SMLReservedWords.ws.P()
    private val newline: RegularExpression = "newline" T (CHAR('\n') X "\\r\\n").P()

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
