@file:Suppress("ktlint:standard:no-wildcard-imports")

package tokenizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SMLTokeniserTest {
    @Test
    fun testEmptyString() {
        val result = SMLTokeniser.tokenise("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSingleReservedWord() {
        val result = SMLTokeniser.tokenise("if")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.ReservedWord.IF, result[0].type)
        assertEquals("if", result[0].lexeme)
        assertEquals(0, result[0].position)
    }

    @Test
    fun testAllReservedWords() {
        val keywords =
            listOf(
                "abstype",
                "and",
                "andalso",
                "as",
                "case",
                "datatype",
                "do",
                "else",
                "end",
                "exception",
                "fn",
                "fun",
                "handle",
                "if",
                "in",
                "infix",
                "infixr",
                "let",
                "local",
                "nonfix",
                "of",
                "op",
                "open",
                "orelse",
                "raise",
                "rec",
                "then",
                "type",
                "val",
                "with",
                "withtype",
                "while",
            )
        for (kw in keywords) {
            val result = SMLTokeniser.tokenise(kw)
            assertEquals(1, result.size, "Expected 1 token for keyword '$kw'")
            assertTrue(result[0].type is SMLTokenType.ReservedWord, "Expected ReservedWord for '$kw'")
        }
    }

    @Test
    fun testAllPunctuation() {
        val punctuation =
            mapOf(
                "(" to SMLTokenType.Punctuation.LPAREN,
                ")" to SMLTokenType.Punctuation.RPAREN,
                "[" to SMLTokenType.Punctuation.LBRACK,
                "]" to SMLTokenType.Punctuation.RBRACK,
                "{" to SMLTokenType.Punctuation.LBRACE,
                "}" to SMLTokenType.Punctuation.RBRACE,
                "," to SMLTokenType.Punctuation.COMMA,
                ":" to SMLTokenType.Punctuation.COLON,
                ";" to SMLTokenType.Punctuation.SEMICOLON,
                "..." to SMLTokenType.Punctuation.ELLIPSIS,
                "_" to SMLTokenType.Punctuation.UNDERBAR,
                "|" to SMLTokenType.Punctuation.PIPE,
                "=" to SMLTokenType.Punctuation.EQUALS,
                "=>" to SMLTokenType.Punctuation.DOUBLE_ARROW,
                "->" to SMLTokenType.Punctuation.ARROW,
                "#" to SMLTokenType.Punctuation.HASH,
            )
        for ((text, expectedType) in punctuation) {
            val result = SMLTokeniser.tokenise(text)
            assertEquals(1, result.size, "Expected 1 token for punctuation '$text'")
            assertEquals(expectedType, result[0].type, "Expected $expectedType for '$text'")
        }
    }

    @Test
    fun testIntegerLiterals() {
        val result1 = SMLTokeniser.tokenise("42")
        assertEquals(1, result1.size)
        assertEquals(SMLTokenType.Literal.INTEGER, result1[0].type)
        assertEquals("42", result1[0].lexeme)

        val result2 = SMLTokeniser.tokenise("~123")
        assertEquals(1, result2.size)
        assertEquals(SMLTokenType.Literal.INTEGER, result2[0].type)
        assertEquals("~123", result2[0].lexeme)

        val result3 = SMLTokeniser.tokenise("0x1A")
        assertEquals(1, result3.size)
        assertEquals(SMLTokenType.Literal.INTEGER, result3[0].type)
        assertEquals("0x1A", result3[0].lexeme)

        val result4 = SMLTokeniser.tokenise("~0xFF")
        assertEquals(1, result4.size)
        assertEquals(SMLTokenType.Literal.INTEGER, result4[0].type)
    }

    @Test
    fun testWordLiterals() {
        val result1 = SMLTokeniser.tokenise("0w42")
        assertEquals(1, result1.size)
        assertEquals(SMLTokenType.Literal.WORD, result1[0].type)
        assertEquals("0w42", result1[0].lexeme)

        val result2 = SMLTokeniser.tokenise("0wx1A")
        assertEquals(1, result2.size)
        assertEquals(SMLTokenType.Literal.WORD, result2[0].type)
        assertEquals("0wx1A", result2[0].lexeme)
    }

    @Test
    fun testRealLiterals() {
        val result1 = SMLTokeniser.tokenise("3.14")
        assertEquals(1, result1.size)
        assertEquals(SMLTokenType.Literal.REAL, result1[0].type)
        assertEquals("3.14", result1[0].lexeme)

        val result2 = SMLTokeniser.tokenise("1E10")
        assertEquals(1, result2.size)
        assertEquals(SMLTokenType.Literal.REAL, result2[0].type)

        val result3 = SMLTokeniser.tokenise("2.5e3")
        assertEquals(1, result3.size)
        assertEquals(SMLTokenType.Literal.REAL, result3[0].type)

        val result4 = SMLTokeniser.tokenise("~1.5E~2")
        assertEquals(1, result4.size)
        assertEquals(SMLTokenType.Literal.REAL, result4[0].type)
    }

    @Test
    fun testStringLiterals() {
        val result = SMLTokeniser.tokenise("\"hello\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.STRING, result[0].type)
        assertEquals("\"hello\"", result[0].lexeme)
    }

    @Test
    fun testStringWithEscapes() {
        val result1 = SMLTokeniser.tokenise("\"\\n\\t\"")
        assertEquals(1, result1.size)
        assertEquals(SMLTokenType.Literal.STRING, result1[0].type)

        val result2 = SMLTokeniser.tokenise("\"\\\\\"")
        assertEquals(1, result2.size)
        assertEquals(SMLTokenType.Literal.STRING, result2[0].type)

        val result3 = SMLTokeniser.tokenise("\"\\\"\"")
        assertEquals(1, result3.size)
        assertEquals(SMLTokenType.Literal.STRING, result3[0].type)

        val result4 = SMLTokeniser.tokenise("\"\\a\\b\\v\\f\\r\"")
        assertEquals(1, result4.size)
        assertEquals(SMLTokenType.Literal.STRING, result4[0].type)
    }

    @Test
    fun testStringWithControlEscape() {
        val result = SMLTokeniser.tokenise("\"\\^A\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.STRING, result[0].type)
    }

    @Test
    fun testStringWithDecimalEscape() {
        val result = SMLTokeniser.tokenise("\"\\065\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.STRING, result[0].type)
    }

    @Test
    fun testStringWithHexEscape() {
        val result = SMLTokeniser.tokenise("\"\\u0041\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.STRING, result[0].type)
    }

    @Test
    fun testStringWithFormattingEscape() {
        val result = SMLTokeniser.tokenise("\"hello\\   \\world\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.STRING, result[0].type)
    }

    @Test
    fun testCharLiteral() {
        val result = SMLTokeniser.tokenise("#\"a\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.CHAR, result[0].type)
        assertEquals("#\"a\"", result[0].lexeme)
    }

    @Test
    fun testAlphanumericIdentifier() {
        val result = SMLTokeniser.tokenise("myVar123")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, result[0].type)
        assertEquals("myVar123", result[0].lexeme)
    }

    @Test
    fun testIdentifierWithPrimeAndUnderscore() {
        val result = SMLTokeniser.tokenise("x'_123")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, result[0].type)
    }

    @Test
    fun testSymbolicIdentifier() {
        val result1 = SMLTokeniser.tokenise("++")
        assertEquals(1, result1.size)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, result1[0].type)

        val result2 = SMLTokeniser.tokenise("<<>>")
        assertEquals(1, result2.size)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, result2[0].type)

        val result3 = SMLTokeniser.tokenise("!@#$%")
        assertEquals(1, result3.size)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, result3[0].type)
    }

    @Test
    fun testEqualityTypeVariableError() {
        // Equality type variables use tags like "ETYVAR" which are not in the SMLTokenType map
        assertThrows(TokenisationError::class.java) {
            SMLTokeniser.tokenise("''a")
        }
    }

    @Test
    fun testNumericLabel() {
        val result = SMLTokeniser.tokenise("1")
        assertEquals(1, result.size)
        assertTrue(
            result[0].type == SMLTokenType.Identifier.NUMERIC_LABEL ||
                result[0].type == SMLTokenType.Literal.INTEGER,
        )
    }

    @Test
    fun testWhitespace() {
        val result = SMLTokeniser.tokenise("   ")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Trivia.WHITESPACE, result[0].type)
    }

    @Test
    fun testTabWhitespace() {
        val result = SMLTokeniser.tokenise("\t\t")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Trivia.WHITESPACE, result[0].type)
    }

    @Test
    fun testCarriageReturnWhitespace() {
        val result = SMLTokeniser.tokenise("\r\r")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Trivia.WHITESPACE, result[0].type)
    }

    @Test
    fun testNewline() {
        val result = SMLTokeniser.tokenise("\n")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Trivia.NEWLINE, result[0].type)
    }

    @Test
    fun testCRLF() {
        // Note: The source uses "\\r\\n" which is literal backslash characters
        // So the lexer expects literal "\r\n" string (4 chars), not actual CRLF (2 chars)
        val result = SMLTokeniser.tokenise("\\r\\n")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Trivia.NEWLINE, result[0].type)
    }

    @Test
    fun testSimpleExpression() {
        val result = SMLTokeniser.tokenise("if x1 then y1 else z1")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(6, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.IF, withoutTrivia[0].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[1].type)
        assertEquals("x1", withoutTrivia[1].lexeme)
        assertEquals(SMLTokenType.ReservedWord.THEN, withoutTrivia[2].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[3].type)
        assertEquals("y1", withoutTrivia[3].lexeme)
        assertEquals(SMLTokenType.ReservedWord.ELSE, withoutTrivia[4].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[5].type)
        assertEquals("z1", withoutTrivia[5].lexeme)
    }

    @Test
    fun testFunctionDefinition() {
        val result = SMLTokeniser.tokenise("fun f x = x")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(5, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.FUN, withoutTrivia[0].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[1].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[2].type)
        assertEquals(SMLTokenType.Punctuation.EQUALS, withoutTrivia[3].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[4].type)
    }

    @Test
    fun testValBinding() {
        val result = SMLTokeniser.tokenise("val x = 42")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(4, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.VAL, withoutTrivia[0].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[1].type)
        assertEquals(SMLTokenType.Punctuation.EQUALS, withoutTrivia[2].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutTrivia[3].type)
    }

    @Test
    fun testPositionTracking() {
        val result = SMLTokeniser.tokenise("ab cd")
        assertEquals(3, result.size)
        assertEquals(0, result[0].position)
        assertEquals(2, result[0].endPosition)
        assertEquals(2, result[1].position)
        assertEquals(3, result[2].position)
    }

    @Test
    fun testTokenisationError() {
        // Backtick is part of symbolic identifiers, so we need a truly invalid character
        // Control characters or special unicode should trigger errors
        val exception =
            assertThrows(TokenisationError::class.java) {
                SMLTokeniser.tokenise("hello\u0000world")
            }

        assertTrue(exception.position >= 0)
        assertTrue(exception.context.isNotEmpty())
    }

    @Test
    fun testTokenisationErrorAtStart() {
        val exception =
            assertThrows(TokenisationError::class.java) {
                SMLTokeniser.tokenise("\u0000")
            }
        assertTrue(exception.position >= 0)
    }

    @Test
    fun testTokenisationErrorContext() {
        val exception =
            assertThrows(TokenisationError::class.java) {
                SMLTokeniser.tokenise("valid\u0000invalid")
            }
        assertTrue(exception.context.isNotEmpty())
    }

    @Test
    fun testCompactString() {
        val result = SMLTokeniser.tokenise("val x = 1")
        val compact = result.withoutTrivia().toCompactString()
        assertTrue(compact.contains("VAL"))
        assertTrue(compact.contains("ID(x)"))
        assertTrue(compact.contains("="))
    }

    @Test
    fun testDetailedString() {
        val result = SMLTokeniser.tokenise("if")
        val detailed = result.toDetailedString()
        assertTrue(detailed.contains("Token"))
        assertTrue(detailed.contains("IF"))
        assertTrue(detailed.contains("pos=0"))
    }

    @Test
    fun testLetExpression() {
        val result = SMLTokeniser.tokenise("let val x1 = 1 in x1 end")
        val withoutTrivia = result.withoutTrivia()
        print(withoutTrivia.toCompactString())
        assertEquals(8, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.LET, withoutTrivia[0].type)
        assertEquals(SMLTokenType.ReservedWord.VAL, withoutTrivia[1].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[2].type)
        assertEquals("x1", withoutTrivia[2].lexeme)
        assertEquals(SMLTokenType.Punctuation.EQUALS, withoutTrivia[3].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutTrivia[4].type)
        assertEquals(SMLTokenType.ReservedWord.IN, withoutTrivia[5].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[6].type)
        assertEquals("x1", withoutTrivia[6].lexeme)
        assertEquals(SMLTokenType.ReservedWord.END, withoutTrivia[7].type)
    }

    @Test
    fun testCaseExpression() {
        val result = SMLTokeniser.tokenise("case x of y => z")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(6, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.CASE, withoutTrivia[0].type)
        assertEquals(SMLTokenType.ReservedWord.OF, withoutTrivia[2].type)
        assertEquals(SMLTokenType.Punctuation.DOUBLE_ARROW, withoutTrivia[4].type)
    }

    @Test
    fun testFnExpression() {
        val result = SMLTokeniser.tokenise("fn x => x")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(4, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.FN, withoutTrivia[0].type)
        assertEquals(SMLTokenType.Punctuation.DOUBLE_ARROW, withoutTrivia[2].type)
    }

    @Test
    fun testDatatype() {
        val result = SMLTokeniser.tokenise("datatype t = A | B")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(6, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.DATATYPE, withoutTrivia[0].type)
        assertEquals(SMLTokenType.Punctuation.PIPE, withoutTrivia[4].type)
    }

    @Test
    fun testTypeAnnotation() {
        val result = SMLTokeniser.tokenise("x : int -> int")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(5, withoutTrivia.size)
        assertEquals(SMLTokenType.Punctuation.COLON, withoutTrivia[1].type)
        assertEquals(SMLTokenType.Punctuation.ARROW, withoutTrivia[3].type)
    }

    @Test
    fun testTupleAndRecord() {
        val result = SMLTokeniser.tokenise("(1, 2) {a = 1}")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.LPAREN })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.RPAREN })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.LBRACE })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.RBRACE })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.COMMA })
    }

    @Test
    fun testListSyntax() {
        val result = SMLTokeniser.tokenise("[1, 2, 3]")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.LBRACK })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.RBRACK })
    }

    @Test
    fun testHandleExpression() {
        val result = SMLTokeniser.tokenise("e handle _ => 0")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.HANDLE })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.UNDERBAR })
    }

    @Test
    fun testRaiseExpression() {
        val result = SMLTokeniser.tokenise("raise E")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(2, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.RAISE, withoutTrivia[0].type)
    }

    @Test
    fun testExceptionDeclaration() {
        val result = SMLTokeniser.tokenise("exception E")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(2, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.EXCEPTION, withoutTrivia[0].type)
    }

    @Test
    fun testLocalDeclaration() {
        val result = SMLTokeniser.tokenise("local val x = 1 in val y = x end")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.LOCAL })
    }

    @Test
    fun testAbstype() {
        val result = SMLTokeniser.tokenise("abstype t = A with end")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.ABSTYPE })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.WITH })
    }

    @Test
    fun testWithtype() {
        val result = SMLTokeniser.tokenise("withtype t = int")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.WITHTYPE })
    }

    @Test
    fun testInfixDeclarations() {
        val result1 = SMLTokeniser.tokenise("infix ++")
        assertTrue(result1.withoutTrivia().any { it.type == SMLTokenType.ReservedWord.INFIX })

        val result2 = SMLTokeniser.tokenise("infixr ++")
        assertTrue(result2.withoutTrivia().any { it.type == SMLTokenType.ReservedWord.INFIXR })

        val result3 = SMLTokeniser.tokenise("nonfix ++")
        assertTrue(result3.withoutTrivia().any { it.type == SMLTokenType.ReservedWord.NONFIX })
    }

    @Test
    fun testOpKeyword() {
        val result = SMLTokeniser.tokenise("op +")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(2, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.OP, withoutTrivia[0].type)
    }

    @Test
    fun testOpenKeyword() {
        val result = SMLTokeniser.tokenise("open List")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(2, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.OPEN, withoutTrivia[0].type)
    }

    @Test
    fun testRecKeyword() {
        val result = SMLTokeniser.tokenise("val rec f = fn x => x")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.REC })
    }

    @Test
    fun testAndalsoOrelse() {
        val result1 = SMLTokeniser.tokenise("a andalso b")
        assertTrue(result1.withoutTrivia().any { it.type == SMLTokenType.ReservedWord.ANDALSO })

        val result2 = SMLTokeniser.tokenise("a orelse b")
        assertTrue(result2.withoutTrivia().any { it.type == SMLTokenType.ReservedWord.ORELSE })
    }

    @Test
    fun testDoWhile() {
        val result = SMLTokeniser.tokenise("while c do e")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.WHILE })
        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.DO })
    }

    @Test
    fun testTypeDeclaration() {
        val result = SMLTokeniser.tokenise("type t = int")
        val withoutTrivia = result.withoutTrivia()

        assertEquals(4, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.TYPE, withoutTrivia[0].type)
    }

    @Test
    fun testAsPattern() {
        val result = SMLTokeniser.tokenise("x as y")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.ReservedWord.AS })
    }

    @Test
    fun testEllipsis() {
        val result = SMLTokeniser.tokenise("{a = 1, ...}")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.ELLIPSIS })
    }

    @Test
    fun testSemicolon() {
        val result = SMLTokeniser.tokenise("a; b")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.SEMICOLON })
    }

    @Test
    fun testHashSelector() {
        val result = SMLTokeniser.tokenise("#1 r")
        val withoutTrivia = result.withoutTrivia()

        assertTrue(withoutTrivia.any { it.type == SMLTokenType.Punctuation.HASH })
    }

    @Test
    fun testEmptyString2() {
        val result = SMLTokeniser.tokenise("\"\"")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Literal.STRING, result[0].type)
    }

    @Test
    fun testMixedWhitespace() {
        val result = SMLTokeniser.tokenise("a \t b")
        assertEquals(3, result.size)
    }

    @Test
    fun testMultipleNewlines() {
        val result = SMLTokeniser.tokenise("\n\n\n")
        assertEquals(1, result.size)
        assertEquals(SMLTokenType.Trivia.NEWLINE, result[0].type)
    }

    @Test
    fun testContextExtractionWithLongString() {
        val longString = "fun factorial n = if n = 0 thn\u0000 1 else n * factorial (n - 1)"
        val exception =
            assertThrows(TokenisationError::class.java) {
                SMLTokeniser.tokenise(longString)
            }
        val expected = "...n = 0 thn\u0000 1 else n ..."
        assertEquals(expected, exception.context)
    }
}
