package comparison

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TokenDiffTest {

    @Test
    fun `oracle only - token in oracle not in polylex`() {
        val oracle = listOf("VAL", "ID(x)", "=", "INT(42)")
        val polylex = listOf("VAL", "ID(x)", "=")
        val mismatches = TokenDiff.diff(oracle, polylex)
        val oracleOnly = mismatches.firstOrNull { it.type == MismatchType.ORACLE_ONLY }
        assertNotNull(oracleOnly, "Expected at least one ORACLE_ONLY mismatch")
        assertEquals("INT(42)", oracleOnly.oracleToken)
        assertEquals(null, oracleOnly.polylexToken)
    }

    @Test
    fun `polylex only - token in polylex not in oracle`() {
        val oracle = listOf("VAL", "ID(x)")
        val polylex = listOf("VAL", "ID(x)", "=", "INT(42)")
        val mismatches = TokenDiff.diff(oracle, polylex)
        val polylexOnly = mismatches.firstOrNull { it.type == MismatchType.POLYLEX_ONLY && it.polylexToken == "INT(42)" }
        assertNotNull(polylexOnly, "Expected a POLYLEX_ONLY mismatch for INT(42)")
        assertEquals("INT(42)", polylexOnly.polylexToken)
        assertEquals(null, polylexOnly.oracleToken)
    }

    @Test
    fun `token type mismatch - same position different type tag`() {
        val oracle = listOf("VAL", "ID(x)", "=", "INT(42)")
        val polylex = listOf("VAL", "ID(x)", "=", "REAL(42)")
        val mismatches = TokenDiff.diff(oracle, polylex)
        val typeMismatch = mismatches.firstOrNull { it.type == MismatchType.TOKEN_TYPE_MISMATCH }
        assertNotNull(typeMismatch, "Expected a TOKEN_TYPE_MISMATCH")
        assertEquals("INT(42)", typeMismatch.oracleToken)
        assertEquals("REAL(42)", typeMismatch.polylexToken)
    }

    @Test
    fun `token text mismatch - same type tag different lexeme`() {
        val oracle = listOf("VAL", "ID(x)", "=", "INT(42)")
        val polylex = listOf("VAL", "ID(x)", "=", "INT(99)")
        val mismatches = TokenDiff.diff(oracle, polylex)
        val textMismatch = mismatches.firstOrNull { it.type == MismatchType.TOKEN_TEXT_MISMATCH }
        assertNotNull(textMismatch, "Expected a TOKEN_TEXT_MISMATCH")
        assertEquals("INT(42)", textMismatch.oracleToken)
        assertEquals("INT(99)", textMismatch.polylexToken)
    }

    @Test
    fun `wrong token position - correct token at different index`() {
        val oracle = listOf("VAL", "ID(x)", "=")
        val polylex = listOf("=", "VAL", "ID(x)")
        val mismatches = TokenDiff.diff(oracle, polylex)
        val positionMismatch = mismatches.firstOrNull { it.type == MismatchType.WRONG_TOKEN_POSITION }
        assertNotNull(positionMismatch, "Expected at least one WRONG_TOKEN_POSITION mismatch")
    }

    @Test
    fun `identical lists produce empty mismatches`() {
        val oracle = listOf("VAL", "ID(x)", "=", "INT(42)")
        val polylex = listOf("VAL", "ID(x)", "=", "INT(42)")
        val mismatches = TokenDiff.diff(oracle, polylex)
        assertTrue(mismatches.isEmpty(), "Identical token lists should produce no mismatches")
    }

    @Test
    fun `both empty lists produce empty mismatches`() {
        val mismatches = TokenDiff.diff(emptyList(), emptyList())
        assertTrue(mismatches.isEmpty(), "Empty token lists should produce no mismatches")
    }

    @Test
    fun `bare tags classified correctly`() {
        val oracle = listOf("VAL", "=")
        val polylex = listOf("FUN", "=")
        val mismatches = TokenDiff.diff(oracle, polylex)
        val typeMismatch = mismatches.firstOrNull { it.type == MismatchType.TOKEN_TYPE_MISMATCH }
        assertNotNull(typeMismatch, "Expected TOKEN_TYPE_MISMATCH for VAL vs FUN (both bare tags)")
    }

    // --- lcs direct tests ---

    @Test
    fun `lcs with both empty lists returns empty`() {
        assertEquals(emptyList<Pair<Int, Int>>(), lcs(emptyList(), emptyList()))
    }

    @Test
    fun `lcs with first list empty returns empty`() {
        assertEquals(emptyList<Pair<Int, Int>>(), lcs(emptyList(), listOf("A")))
    }

    @Test
    fun `lcs with second list empty returns empty`() {
        assertEquals(emptyList<Pair<Int, Int>>(), lcs(listOf("A"), emptyList()))
    }

    @Test
    fun `lcs with no common elements returns empty`() {
        assertEquals(emptyList<Pair<Int, Int>>(), lcs(listOf("A", "B"), listOf("C", "D")))
    }

    @Test
    fun `lcs with identical lists returns all indices paired`() {
        val result = lcs(listOf("A", "B", "C"), listOf("A", "B", "C"))
        assertEquals(listOf(0 to 0, 1 to 1, 2 to 2), result)
    }

    @Test
    fun `lcs finds common subsequence and exercises both backtrack branches`() {
        // Designed so backtracking hits both dp[i-1][j] > dp[i][j-1] (i-- branch)
        // and the else (j-- branch). See DP table analysis in test design.
        val result = lcs(listOf("A", "B", "C", "D"), listOf("B", "A", "D", "C"))
        assertEquals(listOf(1 to 0, 3 to 2), result)
    }

    @Test
    fun `lcs with single common element`() {
        val result = lcs(listOf("X", "A", "Y"), listOf("P", "A", "Q"))
        assertEquals(listOf(1 to 1), result)
    }

    // --- parseCompactToken direct tests ---

    @Test
    fun `parseCompactToken with bare tag returns null lexeme`() {
        assertEquals(CompactToken("VAL", null), parseCompactToken("VAL"))
    }

    @Test
    fun `parseCompactToken with lexeme extracts type tag and lexeme`() {
        assertEquals(CompactToken("INT", "42"), parseCompactToken("INT(42)"))
    }

    @Test
    fun `parseCompactToken with empty lexeme`() {
        assertEquals(CompactToken("EMPTY", ""), parseCompactToken("EMPTY()"))
    }

    @Test
    fun `parseCompactToken with complex lexeme`() {
        assertEquals(CompactToken("STRING", "hello world"), parseCompactToken("STRING(hello world)"))
    }

    // --- classifySubstitution direct tests ---

    @Test
    fun `classifySubstitution returns TOKEN_TYPE_MISMATCH for different type tags`() {
        assertEquals(MismatchType.TOKEN_TYPE_MISMATCH, classifySubstitution("INT(42)", "REAL(42)"))
    }

    @Test
    fun `classifySubstitution returns TOKEN_TEXT_MISMATCH for same type different lexeme`() {
        assertEquals(MismatchType.TOKEN_TEXT_MISMATCH, classifySubstitution("INT(42)", "INT(99)"))
    }

    @Test
    fun `classifySubstitution returns WRONG_TOKEN_POSITION for identical tokens`() {
        assertEquals(MismatchType.WRONG_TOKEN_POSITION, classifySubstitution("INT(42)", "INT(42)"))
    }

    @Test
    fun `classifySubstitution returns TOKEN_TYPE_MISMATCH for different bare tags`() {
        assertEquals(MismatchType.TOKEN_TYPE_MISMATCH, classifySubstitution("VAL", "FUN"))
    }

    @Test
    fun `classifySubstitution returns WRONG_TOKEN_POSITION for identical bare tags`() {
        assertEquals(MismatchType.WRONG_TOKEN_POSITION, classifySubstitution("VAL", "VAL"))
    }

    // --- diff edge cases ---

    @Test
    fun `diff with oracle empty and polylex non-empty produces all POLYLEX_ONLY`() {
        val mismatches = TokenDiff.diff(emptyList(), listOf("A", "B"))
        assertEquals(2, mismatches.size)
        assertTrue(mismatches.all { it.type == MismatchType.POLYLEX_ONLY })
        assertTrue(mismatches.all { it.oracleIndex == -1 && it.oracleToken == null })
    }

    @Test
    fun `diff with polylex empty and oracle non-empty produces all ORACLE_ONLY`() {
        val mismatches = TokenDiff.diff(listOf("A", "B"), emptyList())
        assertEquals(2, mismatches.size)
        assertTrue(mismatches.all { it.type == MismatchType.ORACLE_ONLY })
        assertTrue(mismatches.all { it.polylexIndex == -1 && it.polylexToken == null })
    }

    @Test
    fun `diff with completely different lists produces substitutions`() {
        // No common tokens, so all are paired as substitutions in a single gap region
        val mismatches = TokenDiff.diff(listOf("A", "B"), listOf("C", "D"))
        assertEquals(2, mismatches.size)
        assertTrue(mismatches.all { it.type == MismatchType.TOKEN_TYPE_MISMATCH })
    }

    @Test
    fun `diff with more oracle unmatched in gap produces substitutions and ORACLE_ONLY`() {
        // LCS anchors: A(0,0), B(4,2). Gap: oracle [1,2,3], polylex [1].
        // X<->W paired as substitution, Y and Z become ORACLE_ONLY
        val oracle = listOf("A", "X", "Y", "Z", "B")
        val polylex = listOf("A", "W", "B")
        val mismatches = TokenDiff.diff(oracle, polylex)

        val substitution = mismatches.filter { it.type == MismatchType.TOKEN_TYPE_MISMATCH }
        val oracleOnly = mismatches.filter { it.type == MismatchType.ORACLE_ONLY }
        val wrongPos = mismatches.filter { it.type == MismatchType.WRONG_TOKEN_POSITION }

        assertEquals(1, substitution.size, "X<->W should be a substitution")
        assertEquals("X", substitution[0].oracleToken)
        assertEquals("W", substitution[0].polylexToken)
        assertEquals(2, oracleOnly.size, "Y and Z should be ORACLE_ONLY")
        // B matched at different positions (4 vs 2) → WRONG_TOKEN_POSITION
        assertEquals(1, wrongPos.size, "B at different positions should be WRONG_TOKEN_POSITION")
    }

    @Test
    fun `diff with more polylex unmatched in gap produces substitutions and POLYLEX_ONLY`() {
        // Symmetric to above: more polylex unmatched in the gap
        val oracle = listOf("A", "W", "B")
        val polylex = listOf("A", "X", "Y", "Z", "B")
        val mismatches = TokenDiff.diff(oracle, polylex)

        val substitution = mismatches.filter { it.type == MismatchType.TOKEN_TYPE_MISMATCH }
        val polylexOnly = mismatches.filter { it.type == MismatchType.POLYLEX_ONLY }

        assertEquals(1, substitution.size, "W<->X should be a substitution")
        assertEquals(2, polylexOnly.size, "Y and Z should be POLYLEX_ONLY")
    }

    @Test
    fun `diff with single element lists that differ`() {
        val mismatches = TokenDiff.diff(listOf("A"), listOf("B"))
        assertEquals(1, mismatches.size)
        assertEquals(MismatchType.TOKEN_TYPE_MISMATCH, mismatches[0].type)
        assertEquals("A", mismatches[0].oracleToken)
        assertEquals("B", mismatches[0].polylexToken)
    }
}
