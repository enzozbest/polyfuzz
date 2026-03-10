package comparison

object ComparisonEngine {
    /**
     * Compare two token stream strings, where verilex (oracle) is always the correct reference.
     *
     * Token streams are always compared regardless of error counts. Both invokers strip error
     * tokens before building the stream, so the streams arrive clean and are always comparable.
     *
     * @param oracleStream  Token stream from verilex (compact format, space-separated, ERROR-free)
     * @param polylexStream Token stream from polylex (compact format, space-separated, error-line-free)
     * @param oracleErrors  Number of ERROR tokens verilex emitted for this input
     * @param polylexErrors Number of "Lex error" lines polylex emitted for this input
     * @return Match if error counts agree and streams are equivalent;
     *         ErrorMismatch if streams are equivalent but error counts differ;
     *         Diff with categorised mismatches if streams differ
     */
    fun compare(
        oracleStream: String,
        polylexStream: String,
        oracleErrors: Int,
        polylexErrors: Int,
    ): ComparisonResult {
        // Normalise both streams: exponent case in REAL tokens is not significant
        val normalisedOracle = normaliseRealExponents(oracleStream)
        val normalisedPolylex = normaliseRealExponents(polylexStream)

        // Short-circuit: O(n) comparison for identical streams, which is the majority of cases.
        if (normalisedOracle == normalisedPolylex) {
            return if (oracleErrors != polylexErrors)
                ComparisonResult.ErrorMismatch(oracleErrors, polylexErrors)
            else
                ComparisonResult.Match
        }

        // Normalise formatting gaps in STRING tokens. Verilex preserves raw source text
        // (including formatting escapes like "\ \"), while polylex collapses them during
        // lexing. Both are valid — the Definition says gaps are ignored.
        val oracleTokens = normalisedOracle.toTokenList().map { collapseFormattingGaps(it) }
        val polylexTokens = normalisedPolylex.toTokenList().map { collapseFormattingGaps(it) }

        if (oracleTokens == polylexTokens) {
            return if (oracleErrors != polylexErrors)
                ComparisonResult.ErrorMismatch(oracleErrors, polylexErrors)
            else
                ComparisonResult.Match
        }

        val mismatches = TokenDiff.diff(oracleTokens, polylexTokens)

        return ComparisonResult.Diff(mismatches)
    }
}

private val REAL_LEXEME = Regex("""(?<=REAL\()[^)]*""")

/**
 * Formatting gap pattern per the Definition of SML: a backslash, one or more formatting
 * characters (space, tab, newline, form feed, carriage return), then a closing backslash.
 * These gaps are semantically ignored in string values.
 */
private val FORMATTING_GAP = Regex("""\\[ \t\n\r\u000C]+\\""")

/**
 * Collapse formatting gaps inside STRING and CHAR token lexemes.
 *
 * Verilex preserves the raw source text (e.g. `STRING("hello\ \world")`), while polylex
 * resolves formatting escapes during lexing (e.g. `STRING("helloworld")`). The Definition
 * says gaps are ignored, so both representations are correct. This normalisation lets the
 * comparison focus on real tokenisation differences.
 */
fun collapseFormattingGaps(token: String): String {
    if (!token.startsWith("STRING(") && !token.startsWith("CHAR(")) return token
    return FORMATTING_GAP.replace(token, "")
}

/**
 * Lowercase the lexeme inside REAL(...) tokens so that exponent case differences (e.g. 1.0E5 vs 1.0e5) don't produce
 * false mismatches. Applied to both streams since the source may use either case and each lexer preserves it verbatim.
 * Only affects the lexeme inside REAL() — the type tag, identifiers, strings, etc. are untouched.
 */
fun normaliseRealExponents(stream: String): String =
    REAL_LEXEME.replace(stream) { it.value.lowercase() }

/**
 * Split a compact token stream string into individual token strings.
 *
 * Handles tokens containing spaces inside parentheses (e.g., STRING("hello world")) by tracking parenthesis depth
 * during the split.
 */
fun String.toTokenList(): List<String> {
    if (this.isBlank()) return emptyList()
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    for (ch in this) {
        when {
            ch == '(' -> { depth++; current.append(ch) }
            ch == ')' -> { depth--; current.append(ch) }
            ch == ' ' && depth == 0 -> {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
            }
            else -> current.append(ch)
        }
    }
    if (current.isNotEmpty()) tokens.add(current.toString())
    return tokens
}
