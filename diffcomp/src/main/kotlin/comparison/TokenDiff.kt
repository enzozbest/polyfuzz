package comparison

/**
 * LCS-based token diff and mismatch classification.
 *
 * Classifies every difference between oracle and polylex token lists into one of five categories:
 *   - ORACLE_ONLY: token present in oracle but absent from polylex
 *   - POLYLEX_ONLY: token present in polylex but absent from oracle
 *   - TOKEN_TYPE_MISMATCH: same position, different type tag
 *   - TOKEN_TEXT_MISMATCH: same position, same type tag, different lexeme
 *   - WRONG_TOKEN_POSITION: identical token matched by LCS but at different indices
 */
object TokenDiff {
    fun diff(oracleTokens: List<String>, polylexTokens: List<String>): List<Mismatch> {
        if (oracleTokens.isEmpty() && polylexTokens.isEmpty()) return emptyList()

        val matchedPairs = lcs(oracleTokens, polylexTokens)
        val matchedOracleIndices = matchedPairs.map { it.first }.toSet()
        val matchedPolylexIndices = matchedPairs.map { it.second }.toSet()

        val mismatches = mutableListOf<Mismatch>()

        // Detect WRONG_TOKEN_POSITION: matched by LCS but at different indices.
        // Only report the first occurrence of each distinct offset to avoid
        // cascading noise after a single insertion or deletion.
        val reportedOffsets = mutableSetOf<Int>()
        for ((i, j) in matchedPairs) {
            if (i != j) {
                val offset = i - j
                if (offset !in reportedOffsets) {
                    reportedOffsets.add(offset)
                    mismatches.add(
                        Mismatch(
                            type = MismatchType.WRONG_TOKEN_POSITION,
                            oracleIndex = i,
                            polylexIndex = j,
                            oracleToken = oracleTokens[i],
                            polylexToken = polylexTokens[j],
                        )
                    )
                }
            }
        }

        // Collect unmatched indices
        val unmatchedOracle = oracleTokens.indices.filter { it !in matchedOracleIndices }.toMutableList()
        val unmatchedPolylex = polylexTokens.indices.filter { it !in matchedPolylexIndices }.toMutableList()

        // Build LCS anchor boundaries: gaps between matched pairs define substitution regions.
        // For each gap region, pair unmatched oracle and polylex indices sequentially as substitutions.
        val anchorBoundaries = buildAnchorBoundaries(matchedPairs, oracleTokens.size, polylexTokens.size)

        val consumedOracleIndices = mutableSetOf<Int>()
        val consumedPolylexIndices = mutableSetOf<Int>()

        for ((oracleRange, polylexRange) in anchorBoundaries) {
            val gapOracle = unmatchedOracle.filter { it in oracleRange && it !in consumedOracleIndices }
            val gapPolylex = unmatchedPolylex.filter { it in polylexRange && it !in consumedPolylexIndices }

            val pairCount = minOf(gapOracle.size, gapPolylex.size)
            for (k in 0 until pairCount) {
                val oIdx = gapOracle[k]
                val pIdx = gapPolylex[k]
                val mismatchType = classifySubstitution(oracleTokens[oIdx], polylexTokens[pIdx])
                mismatches.add(
                    Mismatch(
                        type = mismatchType,
                        oracleIndex = oIdx,
                        polylexIndex = pIdx,
                        oracleToken = oracleTokens[oIdx],
                        polylexToken = polylexTokens[pIdx],
                    )
                )
                consumedOracleIndices.add(oIdx)
                consumedPolylexIndices.add(pIdx)
            }
        }

        // Remaining unmatched oracle indices -> ORACLE_ONLY
        for (i in unmatchedOracle) {
            if (i !in consumedOracleIndices) {
                mismatches.add(
                    Mismatch(
                        type = MismatchType.ORACLE_ONLY,
                        oracleIndex = i,
                        polylexIndex = -1,
                        oracleToken = oracleTokens[i],
                        polylexToken = null,
                    )
                )
            }
        }

        // Remaining unmatched polylex indices -> POLYLEX_ONLY
        for (j in unmatchedPolylex) {
            if (j !in consumedPolylexIndices) {
                mismatches.add(
                    Mismatch(
                        type = MismatchType.POLYLEX_ONLY,
                        oracleIndex = -1,
                        polylexIndex = j,
                        oracleToken = null,
                        polylexToken = polylexTokens[j],
                    )
                )
            }
        }

        return mismatches
    }
}

/**
 * Compact token representation split into type tag and optional lexeme.
 * e.g., "INT(42)" -> CompactToken("INT", "42")
 *        "VAL"    -> CompactToken("VAL", null)
 */
data class CompactToken(val typeTag: String, val lexeme: String?)

fun parseCompactToken(token: String): CompactToken {
    val parenIdx = token.indexOf('(')
    return if (parenIdx == -1 || parenIdx == 0 || !token.endsWith(')')) {
        CompactToken(token, null)  // bare tag: "VAL", "=", "(", ")"
    } else {
        CompactToken(
            typeTag = token.substring(0, parenIdx),
            lexeme = token.substring(parenIdx + 1, token.length - 1)
        )
    }
}

/**
 * Standard DP LCS returning matched index pairs (oracleIndex, polylexIndex).
 *
 * The runtime complexity of this algorithm is O(n + m*k), which degrades to O(n^2) in the worst case
 * (n is the length of the string (including spaces), m is the oracle's token count, and k is polylex's token count).
 * Therefore, this function is only called when we know the strings differ.
 *
 * @param a List of compact token strings from oracle (normalised for REAL exponent case)
 * @param b List of compact token strings from polylex (normalised for REAL exponent case)
 * @return List of matched index pairs (oracleIndex, polylexIndex) representing the longest common subsequence
 * of tokens between a and b.
 */
fun lcs(a: List<String>, b: List<String>): List<Pair<Int, Int>> {
    val rows = a.size
    val cols = b.size

    // Build the DP table
    val dp = Array(rows + 1) { IntArray(cols + 1) }
    for (i in 1..rows) {
        for (j in 1..cols) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1] + 1
            } else {
                maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    // Backtrack to recover the matched index pairs
    val matches = mutableListOf<Pair<Int, Int>>()
    var i = rows
    var j = cols

    while (i > 0 && j > 0) {
        when {
            a[i - 1] == b[j - 1] -> {
                matches.add(i - 1 to j - 1)
                i--
                j--
            }
            dp[i - 1][j] > dp[i][j - 1] -> i--
            else -> j--
        }
    }

    return matches.reversed()
}

/**
 * Classify the relationship between two non-matching tokens at substitution positions.
 */
fun classifySubstitution(oracleToken: String, polylexToken: String): MismatchType {
    val oracle = parseCompactToken(oracleToken)
    val polylex = parseCompactToken(polylexToken)
    return when {
        oracle.typeTag != polylex.typeTag -> MismatchType.TOKEN_TYPE_MISMATCH
        oracle.lexeme != polylex.lexeme -> MismatchType.TOKEN_TEXT_MISMATCH
        else -> MismatchType.WRONG_TOKEN_POSITION  // identical token, different position
    }
}

/**
 * Build gap regions between LCS anchor pairs.
 *
 * Each region is a pair of (oracleRange, polylexRange) within which unmatched tokens can be paired as substitutions.
 */
private fun buildAnchorBoundaries(
    matchedPairs: List<Pair<Int, Int>>,
    oracleSize: Int,
    polylexSize: Int,
): List<Pair<IntRange, IntRange>> {
    val boundaries = mutableListOf<Pair<IntRange, IntRange>>()

    // Add sentinel anchors at beginning and end
    val anchors = mutableListOf((-1 to -1)) + matchedPairs + listOf(oracleSize to polylexSize)

    for (k in 0 until anchors.size - 1) {
        val (oi, pi) = anchors[k]
        val (oj, pj) = anchors[k + 1]

        // Gap oracle: (oi+1) until oj, gap polylex range: (pi+1) until pj
        val oracleRange = (oi + 1) until oj
        val polylexRange = (pi + 1) until pj
        if (!oracleRange.isEmpty() || !polylexRange.isEmpty()) {
            boundaries.add(oracleRange to polylexRange)
        }
    }

    return boundaries
}
