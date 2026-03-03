package comparison

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComparisonEngineTest {

    @Test
    fun `identical streams return Match`() {
        val result = ComparisonEngine.compare("VAL ID(x) = INT(42)", "VAL ID(x) = INT(42)")
        assertIs<ComparisonResult.Match>(result)
    }

    @Test
    fun `different streams return Diff`() {
        val result = ComparisonEngine.compare("VAL ID(x)", "VAL ID(y)")
        assertIs<ComparisonResult.Diff>(result)
    }

    @Test
    fun `empty identical streams return Match`() {
        val result = ComparisonEngine.compare("", "")
        assertIs<ComparisonResult.Match>(result)
    }

    @Test
    fun `oracle invariant - Diff never empty for different streams`() {
        val result = ComparisonEngine.compare("VAL ID(x)", "VAL ID(y)")
        assertIs<ComparisonResult.Diff>(result)
        assertTrue(result.mismatches.isNotEmpty(), "Diff should contain at least one mismatch")
    }

    @Test
    fun `oracle invariant - ORACLE_ONLY has null polylexToken`() {
        // oracle has an extra token that polylex does not have
        val result = ComparisonEngine.compare("VAL ID(x) INT(42)", "VAL ID(x)")
        assertIs<ComparisonResult.Diff>(result)
        val oracleOnlyMismatch = result.mismatches.firstOrNull { it.type == MismatchType.ORACLE_ONLY }
        assertNotNull(oracleOnlyMismatch, "Expected an ORACLE_ONLY mismatch")
        assertEquals(null, oracleOnlyMismatch.polylexToken, "ORACLE_ONLY mismatch must have null polylexToken")
    }

    @Test
    fun `oracle invariant - POLYLEX_ONLY has null oracleToken`() {
        // polylex has an extra token that oracle does not have
        val result = ComparisonEngine.compare("VAL ID(x)", "VAL ID(x) INT(42)")
        assertIs<ComparisonResult.Diff>(result)
        val polylexOnlyMismatch = result.mismatches.firstOrNull { it.type == MismatchType.POLYLEX_ONLY }
        assertNotNull(polylexOnlyMismatch, "Expected a POLYLEX_ONLY mismatch")
        assertEquals(null, polylexOnlyMismatch.oracleToken, "POLYLEX_ONLY mismatch must have null oracleToken")
    }

    // --- normaliseRealExponents coverage ---

    @Test
    fun `normaliseRealExponents lowercases REAL lexeme`() {
        assertEquals("REAL(1.0e5)", normaliseRealExponents("REAL(1.0E5)"))
    }

    @Test
    fun `normaliseRealExponents handles multiple REAL tokens`() {
        assertEquals(
            "REAL(1.0e5) REAL(2.0e3)",
            normaliseRealExponents("REAL(1.0E5) REAL(2.0E3)")
        )
    }

    @Test
    fun `normaliseRealExponents preserves non-REAL tokens`() {
        assertEquals("INT(42) VAL ID(x)", normaliseRealExponents("INT(42) VAL ID(x)"))
    }

    @Test
    fun `normaliseRealExponents preserves already lowercase REAL`() {
        assertEquals("REAL(1.0e5)", normaliseRealExponents("REAL(1.0e5)"))
    }

    @Test
    fun `compare matches when only REAL exponent case differs`() {
        val result = ComparisonEngine.compare("REAL(1.0E5)", "REAL(1.0e5)")
        assertIs<ComparisonResult.Match>(result)
    }

    // --- toTokenList coverage ---

    @Test
    fun `toTokenList returns empty for empty string`() {
        assertEquals(emptyList<String>(), "".toTokenList())
    }

    @Test
    fun `toTokenList returns empty for blank string`() {
        assertEquals(emptyList<String>(), "   ".toTokenList())
    }

    @Test
    fun `toTokenList splits simple tokens`() {
        assertEquals(listOf("VAL", "ID(x)", "="), "VAL ID(x) =".toTokenList())
    }

    @Test
    fun `toTokenList preserves spaces inside parentheses`() {
        assertEquals(
            listOf("STRING(hello world)", "VAL"),
            "STRING(hello world) VAL".toTokenList()
        )
    }

    @Test
    fun `toTokenList handles leading spaces`() {
        assertEquals(listOf("VAL", "ID(x)"), " VAL ID(x)".toTokenList())
    }

    @Test
    fun `toTokenList handles trailing spaces`() {
        assertEquals(listOf("VAL", "ID(x)"), "VAL ID(x) ".toTokenList())
    }

    @Test
    fun `toTokenList handles consecutive spaces`() {
        assertEquals(listOf("VAL", "ID(x)"), "VAL  ID(x)".toTokenList())
    }

    @Test
    fun `toTokenList handles single token without spaces`() {
        assertEquals(listOf("VAL"), "VAL".toTokenList())
    }

    @Test
    fun `toTokenList handles nested parentheses`() {
        assertEquals(listOf("FN(a(b))", "VAL"), "FN(a(b)) VAL".toTokenList())
    }

    // --- ComparisonResult.Diff.mismatchCount ---

    @Test
    fun `Diff mismatchCount returns number of mismatches`() {
        val diff = ComparisonResult.Diff(
            listOf(
                Mismatch(MismatchType.ORACLE_ONLY, 0, -1, "A", null),
                Mismatch(MismatchType.POLYLEX_ONLY, -1, 0, null, "B"),
            )
        )
        assertEquals(2, diff.mismatchCount)
    }
}
