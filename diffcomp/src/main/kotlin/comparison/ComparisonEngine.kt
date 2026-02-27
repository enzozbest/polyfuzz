package comparison

object ComparisonEngine {
    /**
     * Compare two token stream strings, where verilex (oracle) is always the correct reference.
     *
     * @param oracleStream  Token stream from verilex (compact format, space-separated)
     * @param polylexStream Token stream from polylex (compact format, space-separated)
     * @return Match if streams are equivalent; Diff with categorised mismatches otherwise
     */
    fun compare(oracleStream: String, polylexStream: String): ComparisonResult {
        // Normalise both streams: exponent case in REAL tokens is not significant
        val normalisedOracle = normaliseRealExponents(oracleStream)
        val normalisedPolylex = normaliseRealExponents(polylexStream)

        //Hash-first short-circuit: O(1) comparison for identical streams, which is the majority of cases.
        if (Hasher.equal(normalisedOracle, normalisedPolylex)) return ComparisonResult.Match

        val oracleTokens = normalisedOracle.toTokenList()
        val polylexTokens = normalisedPolylex.toTokenList()
        val mismatches = TokenDiff.diff(oracleTokens, polylexTokens)

        return ComparisonResult.Diff(mismatches)
    }
}

private val REAL_LEXEME = Regex("""(?<=REAL\()[^)]*""")

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
