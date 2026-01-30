@file:Suppress("ktlint:standard:no-wildcard-imports")

package tokenizer

import lexer.Verilex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rexp.*

class SMLRegexesTest {
    @Test
    fun testReservedWordPatterns() {
        assertNotNull(SMLReservedWords.abstype)
        assertNotNull(SMLReservedWords.and)
        assertNotNull(SMLReservedWords.andalso)
        assertNotNull(SMLReservedWords.as_)
        assertNotNull(SMLReservedWords.case)
        assertNotNull(SMLReservedWords.datatype)
        assertNotNull(SMLReservedWords.do_)
        assertNotNull(SMLReservedWords.else_)
        assertNotNull(SMLReservedWords.end)
        assertNotNull(SMLReservedWords.exception)
        assertNotNull(SMLReservedWords.fn)
        assertNotNull(SMLReservedWords.fun_)
        assertNotNull(SMLReservedWords.handle)
        assertNotNull(SMLReservedWords.if_)
        assertNotNull(SMLReservedWords.in_)
        assertNotNull(SMLReservedWords.infix)
        assertNotNull(SMLReservedWords.infixr)
        assertNotNull(SMLReservedWords.let)
        assertNotNull(SMLReservedWords.local)
        assertNotNull(SMLReservedWords.nonfix)
        assertNotNull(SMLReservedWords.of_)
        assertNotNull(SMLReservedWords.op_)
        assertNotNull(SMLReservedWords.open)
        assertNotNull(SMLReservedWords.orelse)
        assertNotNull(SMLReservedWords.raise)
        assertNotNull(SMLReservedWords.rec)
        assertNotNull(SMLReservedWords.then)
        assertNotNull(SMLReservedWords.type)
        assertNotNull(SMLReservedWords.val_)
        assertNotNull(SMLReservedWords.with)
        assertNotNull(SMLReservedWords.withtype)
        assertNotNull(SMLReservedWords.while_)
    }

    @Test
    fun testPunctuationPatterns() {
        assertTrue(SMLReservedWords.lparen is CHAR)
        assertTrue(SMLReservedWords.rparen is CHAR)
        assertTrue(SMLReservedWords.lbrack is CHAR)
        assertTrue(SMLReservedWords.rbrack is CHAR)
        assertTrue(SMLReservedWords.lbrace is CHAR)
        assertTrue(SMLReservedWords.rbrace is CHAR)
        assertTrue(SMLReservedWords.comma is CHAR)
        assertTrue(SMLReservedWords.colon is CHAR)
        assertTrue(SMLReservedWords.semicolon is CHAR)
        assertNotNull(SMLReservedWords.ellipsis)
        assertNotNull(SMLReservedWords.ws)
        assertTrue(SMLReservedWords.underbar is CHAR)
        assertTrue(SMLReservedWords.pipe is CHAR)
        assertTrue(SMLReservedWords.equals is CHAR)
        assertNotNull(SMLReservedWords.doubleArrow)
        assertNotNull(SMLReservedWords.arrow)
        assertTrue(SMLReservedWords.hash is CHAR)
    }

    @Test
    fun testConstantPatterns() {
        assertNotNull(SMLConstants.decimalInteger)
        assertNotNull(SMLConstants.hexInteger)
        assertNotNull(SMLConstants.decimalWord)
        assertNotNull(SMLConstants.hexWord)
        assertNotNull(SMLConstants.real)
    }

    @Test
    fun testStringPatterns() {
        assertNotNull(SMLStrings.string)
        assertNotNull(SMLStrings.char)
    }

    @Test
    fun testIdentifierPatterns() {
        assertNotNull(SMLIdentifiers.alphanumericId)
        assertNotNull(SMLIdentifiers.symbolicId)
        assertNotNull(SMLIdentifiers.identifier)
        assertNotNull(SMLIdentifiers.tyvar)
        assertNotNull(SMLIdentifiers.numericLabel)
    }

    @Test
    fun testTokenHelperPatterns() {
        assertNotNull(SMLTokenHelpers.optionalSign)
        assertNotNull(SMLTokenHelpers.decimalDigit)
        assertNotNull(SMLTokenHelpers.hexDigit)
        assertNotNull(SMLTokenHelpers.exponent)
        assertNotNull(SMLTokenHelpers.fraction)
        assertNotNull(SMLTokenHelpers.realFraction)
        assertNotNull(SMLTokenHelpers.realExponent)
        assertNotNull(SMLTokenHelpers.realFull)
    }

    @Test
    fun testDecimalIntegerMatches() {
        val pattern = ("int" T SMLConstants.decimalInteger).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "42")
        assertEquals(listOf("int" to "42"), result)

        val result2 = Verilex.lex(pattern, "~123")
        assertEquals(listOf("int" to "~123"), result2)
    }

    @Test
    fun testHexIntegerMatches() {
        val pattern = ("int" T SMLConstants.hexInteger).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "0x1A")
        assertEquals(listOf("int" to "0x1A"), result)

        val result2 = Verilex.lex(pattern, "~0xFF")
        assertEquals(listOf("int" to "~0xFF"), result2)
    }

    @Test
    fun testDecimalWordMatches() {
        val pattern = ("word" T SMLConstants.decimalWord).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "0w42")
        assertEquals(listOf("word" to "0w42"), result)
    }

    @Test
    fun testHexWordMatches() {
        val pattern = ("word" T SMLConstants.hexWord).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "0wx1A")
        assertEquals(listOf("word" to "0wx1A"), result)
    }

    @Test
    fun testRealMatches() {
        val pattern = ("real" T SMLConstants.real).toCharFunctionFormat()

        val result1 = Verilex.lex(pattern, "3.14")
        assertEquals(listOf("real" to "3.14"), result1)

        val result2 = Verilex.lex(pattern, "1E10")
        assertEquals(listOf("real" to "1E10"), result2)

        val result3 = Verilex.lex(pattern, "2.5e3")
        assertEquals(listOf("real" to "2.5e3"), result3)
    }

    @Test
    fun testStringMatches() {
        val pattern = ("str" T SMLStrings.string).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "\"hello\"")
        assertEquals(listOf("str" to "\"hello\""), result)
    }

    @Test
    fun testCharMatches() {
        val pattern = ("chr" T SMLStrings.char).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "#\"a\"")
        assertEquals(listOf("chr" to "#\"a\""), result)
    }

    @Test
    fun testAlphanumericIdMatches() {
        val pattern = ("id" T SMLIdentifiers.alphanumericId).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "myVar123")
        assertEquals(listOf("id" to "myVar123"), result)
    }

    @Test
    fun testSymbolicIdMatches() {
        val pattern = ("id" T SMLIdentifiers.symbolicId).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "++")
        assertEquals(listOf("id" to "++"), result)
    }

    @Test
    fun testTyvarMatches() {
        val pattern = ("tyvar" T SMLIdentifiers.tyvar).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "'a")
        assertEquals(listOf("tyvar" to "'a"), result)
    }

    @Test
    fun testNumericLabelMatches() {
        val pattern = ("label" T SMLIdentifiers.numericLabel).toCharFunctionFormat()
        val result = Verilex.lex(pattern, "1")
        assertEquals(listOf("label" to "1"), result)

        val result2 = Verilex.lex(pattern, "123")
        assertEquals(listOf("label" to "123"), result2)
    }

    @Test
    fun testWhitespacePattern() {
        val ws = SMLReservedWords.ws
        assertNotNull(ws)
    }
}
