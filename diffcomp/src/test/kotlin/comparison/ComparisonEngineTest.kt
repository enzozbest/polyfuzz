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
}
