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
}
