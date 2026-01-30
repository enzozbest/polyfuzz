package tokenizer

import rexp.CHAR
import rexp.F
import rexp.ONE
import rexp.P
import rexp.RANGE
import rexp.S
import rexp.X
import rexp.toRegex

object SMLReservedWords {
    /** Reserved Words **/
    val abstype = "abstype".toRegex()
    val and = "and".toRegex()
    val andalso = "andalso".toRegex()
    val as_ = "as".toRegex()
    val case = "case".toRegex()
    val datatype = "datatype".toRegex()
    val do_ = "do".toRegex()
    val else_ = "else".toRegex()
    val end = "end".toRegex()
    val exception = "exception".toRegex()
    val fn = "fn".toRegex()
    val fun_ = "fun".toRegex()
    val handle = "handle".toRegex()
    val if_ = "if".toRegex()
    val in_ = "in".toRegex()
    val infix = "infix".toRegex()
    val infixr = "infixr".toRegex()
    val let = "let".toRegex()
    val local = "local".toRegex()
    val nonfix = "nonfix".toRegex()
    val of_ = "of".toRegex()
    val op_ = "op".toRegex()
    val open = "open".toRegex()
    val orelse = "orelse".toRegex()
    val raise = "raise".toRegex()
    val rec = "rec".toRegex()
    val then = "then".toRegex()
    val type = "type".toRegex()
    val val_ = "val".toRegex()
    val with = "with".toRegex()
    val withtype = "withtype".toRegex()
    val while_ = "while".toRegex()

    /** Reserved words (symbols) **/
    val lparen = CHAR('(')
    val rparen = CHAR(')')
    val lbrack = CHAR('[')
    val rbrack = CHAR(']')
    val lbrace = CHAR('{')
    val rbrace = CHAR('}')
    val comma = CHAR(',')
    val colon = CHAR(':')
    val semicolon = CHAR(';')
    val ellipsis = "...".toRegex()
    val ws = (" " X "\t" X "\r")
    val underbar = CHAR('_')
    val pipe = CHAR('|')
    val equals = CHAR('=')
    val doubleArrow = "=>".toRegex()
    val arrow = "->".toRegex()
    val hash = CHAR('#')
}

object SMLConstants {
    /** Integer Constant (decimal notation) **/
    val decimalInteger = SMLTokenHelpers.optionalSign F SMLTokenHelpers.decimalDigit.P()

    /** Integer Constant (hexadecimal notation)**/
    val hexInteger = SMLTokenHelpers.optionalSign F "0x" F SMLTokenHelpers.hexDigit.P()

    /** Word Constant (decimal notation) **/
    val decimalWord = "0w" F SMLTokenHelpers.decimalDigit.P()

    /** Word Constant (hexadecimal notation) **/
    val hexWord = "0wx" F SMLTokenHelpers.hexDigit.P()

    /** Real Constant **/
    val real by lazy { SMLTokenHelpers.realFull X SMLTokenHelpers.realFraction X SMLTokenHelpers.realExponent }
}

object SMLStrings {
    private val printableChars = RANGE((33.toChar()..126.toChar()).toSet() + ' ' - '"' - '\\')
    private val simpleEscape = CHAR('\\') F RANGE(setOf('a', 'b', 't', 'n', 'v', 'f', 'r', '"', '\\'))
    private val controlEscape = CHAR('\\') F CHAR('^') F RANGE((64.toChar()..95.toChar()).toSet())
    private val decimalEscape =
        CHAR('\\') F SMLTokenHelpers.decimalDigit F SMLTokenHelpers.decimalDigit F SMLTokenHelpers.decimalDigit
    private val hexEscape =
        CHAR('\\') F CHAR('u') F SMLTokenHelpers.hexDigit F SMLTokenHelpers.hexDigit F SMLTokenHelpers.hexDigit F SMLTokenHelpers.hexDigit
    private val formattingChar = RANGE(setOf(' ', '\t', '\n', 12.toChar(), '\r'))
    private val formattingEscape = CHAR('\\') F formattingChar.P() F CHAR('\\')
    private val escapeSequence = simpleEscape X controlEscape X decimalEscape X hexEscape X formattingEscape
    private val stringChar = printableChars X escapeSequence

    /** String constant: double quote, zero or more string chars, double quote */
    val string = CHAR('"') F stringChar.S() F CHAR('"')

    /** Char constant: hash followed by a string constant of size one */
    val char = SMLReservedWords.hash F CHAR('"') F stringChar F CHAR('"')

    // val comment = "(*" F NOT(ALL.S() F ("*)" X "(*") F ALL.S()) F "*)"
}

object SMLIdentifiers {
    private val letter = RANGE(('a'..'z').toSet() + ('A'..'Z'))
    private val digit = RANGE(('0'..'9').toSet())
    private val prime = CHAR('\'')
    private val underbar = CHAR('_')
    private val alphanumeric = letter X digit X prime X underbar
    private val symbolic = RANGE(setOf('!', '%', '&', '$', '#', '+', '-', '/', ':', '<', '=', '>', '?', '@', '\\', '~', '`', '^', '|', '*'))

    /**
     * Alphanumeric identifier: starts with a letter, followed by zero or more alphanumeric characters.
     */
    val alphanumericId = letter F alphanumeric.S()

    /**
     * Qualified identifier: chain of alphanumeric identifiers separated by '.'.
     */
    val qualifiedId = alphanumericId F ("." F alphanumericId).P()

    /** Symbolic identifier: one or more symbolic characters */
    val symbolicId = symbolic.P()

    /** Any identifier (alphanumeric or symbolic) */
    val identifier = alphanumericId X symbolicId

    /** Type variable (TyVar): alphanumeric identifier starting with a prime
     *
     * Note: this version is NOT in accordance with the Definition of SML. Strictly speaking, identifiers starting
     * with '' are of the class Equality Type Variable (EtyVar). However, PolyML does not differentiate between them,
     * and we will allow that as a trivial deviation. We do not want to reject inputs on that basis.
     */
    val tyvar = prime F ((ONE X prime) F alphanumericId)

    /** Numeric label: any numeral not starting with 0 (for record labels) */
    private val nonZeroDigit = RANGE(('1'..'9').toSet())
    val numericLabel = nonZeroDigit F digit.S()
}

object SMLTokenHelpers {
    val optionalSign = ONE X CHAR('~')
    val decimalDigit = RANGE(('0'..'9').toSet())
    val hexDigit = RANGE(('0'..'9').toSet() + ('A'..'F') + ('a'..'f'))
    val exponent by lazy { (CHAR('E') X CHAR('e')) F SMLConstants.decimalInteger }
    val fraction = CHAR('.') F decimalDigit.P()
    val realFraction by lazy { SMLConstants.decimalInteger F fraction }
    val realExponent by lazy { SMLConstants.decimalInteger F exponent }
    val realFull by lazy { SMLConstants.decimalInteger F fraction F exponent }
}
