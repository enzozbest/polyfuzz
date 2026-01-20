package lexer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rexp.*

class VerilexTest {
    @Test
    fun testSimpleLex() {
        val r = "a".toRegex()
        val res = Verilex.lex(r.toCharFunctionFormat(), "a")
        // No tags, so env() should be empty
        assertTrue(res.isEmpty())
    }

    @Test
    fun testTaggedLex() {
        val r = ("tag" T "a").toCharFunctionFormat()
        val res = Verilex.lex(r, "a")
        assertEquals(listOf("tag" to "a"), res)
    }

    @Test
    fun testMultipleTokens() {
        val r1 = "tag1" T "a"
        val r2 = "tag2" T "b"
        val r = (r1 F r2).toCharFunctionFormat()
        val res = Verilex.lex(r, "ab")
        assertEquals(listOf("tag1" to "a", "tag2" to "b"), res)
    }

    @Test
    fun testStarLex() {
        val a = RANGE(('a'..'z').toSet())
        val r = ("word" T a.P()).toCharFunctionFormat()
        val res = Verilex.lex(r, "hello")
        assertEquals(listOf("word" to "hello"), res)
    }

    @Test
    fun testAltLex() {
        val r1 = "tag1" T "a"
        val r2 = "tag2" T "b"
        val r = (r1 X r2).toCharFunctionFormat()

        assertEquals(listOf("tag1" to "a"), Verilex.lex(r, "a"))
        assertEquals(listOf("tag2" to "b"), Verilex.lex(r, "b"))
    }

    @Test
    fun testComplexLexer() {
        // Simple lexer for: [a-z]+ | [0-9]+
        val letters = RANGE(('a'..'z').toSet())
        val digits = RANGE(('0'..'9').toSet())

        val token = ("word" T letters.P()) X ("num" T digits.P())
        val lexer = (token F (" " T " ".toRegex()).S() F token).toCharFunctionFormat()

        val res = Verilex.lex(lexer, "abc 123")
        assertEquals(listOf("word" to "abc", " " to " ", "num" to "123"), res)
    }

    @Test
    fun testLexingError() {
        val r = "a".toRegex().toCharFunctionFormat()
        // Lexing "b" with regex "a" should fail
        assertThrows(IllegalStateException::class.java) {
            Verilex.lex(r, "b")
        }

        // Lexing empty string with non-nullable regex
        assertThrows(IllegalStateException::class.java) {
            Verilex.lex(r, "")
        }
    }

    @Test
    fun testNullableLex() {
        val r = ("tag" T "a".S()).toCharFunctionFormat()
        assertEquals(listOf("tag" to "aaa"), Verilex.lex(r, "aaa"))
        assertEquals(listOf("tag" to ""), Verilex.lex(r, ""))
    }
}
