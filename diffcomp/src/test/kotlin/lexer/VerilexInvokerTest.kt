package lexer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VerilexInvokerTest {

    @Test
    fun `invoke returns Success for valid SML source`() {
        val result = VerilexInvoker.invoke("val x = 1")
        assertIs<LexerResult.Success>(result)
        assertTrue(result.tokenStream.isNotBlank(), "Expected non-blank token stream")
        assertTrue(result.tokenStream.contains("VAL"), "Expected VAL token")
        assertTrue(result.tokenStream.contains("ID(x)"), "Expected ID(x) token")
    }

    @Test
    fun `invoke returns Failure for empty source producing blank token stream`() {
        // Empty source produces no tokens; withoutTrivia() is also empty; toCompactString() is blank
        val result = VerilexInvoker.invoke("")
        assertIs<LexerResult.Failure>(result)
        assertTrue(result.error.contains("empty token stream"), "Expected empty token stream error")
    }

    @Test
    fun `invoke returns Failure for whitespace-only source`() {
        // Whitespace-only source produces only trivia tokens; withoutTrivia() is empty
        val result = VerilexInvoker.invoke("   ")
        assertIs<LexerResult.Failure>(result)
        assertTrue(result.error.contains("empty token stream"), "Expected empty token stream error")
    }

    @Test
    fun `invoke returns Success with errorCount for unrecognised characters`() {
        // Null character is not valid SML but the catch-all rule produces an ERROR token
        val result = VerilexInvoker.invoke("\u0000")
        assertIs<LexerResult.Success>(result)
        assertTrue(result.errorCount > 0, "Expected non-zero errorCount for null byte")
    }

    @Test
    fun `invoke excludes ERROR tokens from token stream`() {
        // Null character produces ERROR token which should be filtered out
        val result = VerilexInvoker.invoke("\u0000")
        assertIs<LexerResult.Success>(result)
        assertTrue(!result.tokenStream.contains("ERROR"), "ERROR tokens should not appear in token stream")
    }

    @Test
    fun `invoke returns zero errorCount for valid SML source`() {
        val result = VerilexInvoker.invoke("val x = 1")
        assertIs<LexerResult.Success>(result)
        assertEquals(0, result.errorCount, "Expected zero errorCount for valid SML")
    }
}
