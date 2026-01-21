package tokenizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SMLTokeniserTest {
    @Test
    fun `Tokenise empty string returns empty sequence`() {
        val tokens = SMLTokeniser.tokenise("")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `Tokenise simple val declaration`() {
        val tokens = "val x = 42".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(4, withoutWs.size)
        assertEquals(SMLTokenType.ReservedWord.VAL, withoutWs[0].type)
        assertEquals("val", withoutWs[0].lexeme)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[1].type)
        assertEquals("x", withoutWs[1].lexeme)
        assertEquals(SMLTokenType.Punctuation.EQUALS, withoutWs[2].type)
        assertEquals("=", withoutWs[2].lexeme)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[3].type)
        assertEquals("42", withoutWs[3].lexeme)
    }

    @Test
    fun `Tokenise function definition`() {
        val tokens = "fun f x = x".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(5, withoutWs.size)
        assertEquals(SMLTokenType.ReservedWord.FUN, withoutWs[0].type)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[1].type)
        assertEquals("f", withoutWs[1].lexeme)
    }

    @Test
    fun `Tokenise if-then-else`() {
        val tokens = "if true then 1 else 0".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(SMLTokenType.ReservedWord.IF, withoutWs[0].type)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[1].type)
        assertEquals("true", withoutWs[1].lexeme)
        assertEquals(SMLTokenType.ReservedWord.THEN, withoutWs[2].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[3].type)
        assertEquals(SMLTokenType.ReservedWord.ELSE, withoutWs[4].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[5].type)
    }

    @Test
    fun `Reserved word priority over identifier`() {
        val tokens = "if".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.ReservedWord.IF, tokens[0].type)
    }

    @Test
    fun `Identifier starting with reserved word prefix`() {
        val tokens = "ifx".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, tokens[0].type)
        assertEquals("ifx", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise string literal`() {
        val tokens = "\"hello\"".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.STRING, tokens[0].type)
        assertEquals("\"hello\"", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise string with escape sequences`() {
        val tokens = "\"hello\\nworld\"".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.STRING, tokens[0].type)
    }

    @Test
    fun `Tokenise char literal`() {
        val tokens = "#\"a\"".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.CHAR, tokens[0].type)
        assertEquals("#\"a\"", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise type variable`() {
        val tokens = "'a".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Identifier.TYVAR, tokens[0].type)
        assertEquals("'a", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise equality type variable`() {
        val tokens = "''a".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Identifier.ETYVAR, tokens[0].type)
        assertEquals("''a", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise real number`() {
        val tokens = "3.14".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.REAL, tokens[0].type)
        assertEquals("3.14", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise real with exponent`() {
        val tokens = "1E10".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.REAL, tokens[0].type)
    }

    @Test
    fun `Tokenise negative integer`() {
        val tokens = "~42".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.INTEGER, tokens[0].type)
        assertEquals("~42", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise hex integer`() {
        val tokens = "0xFF".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.HEX_INTEGER, tokens[0].type)
        assertEquals("0xFF", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise word`() {
        val tokens = "0w42".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.WORD, tokens[0].type)
        assertEquals("0w42", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise hex word`() {
        val tokens = "0wxFF".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Literal.HEX_WORD, tokens[0].type)
        assertEquals("0wxFF", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise symbolic identifier`() {
        val tokens = "++".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Identifier.SYMBOLIC_ID, tokens[0].type)
        assertEquals("++", tokens[0].lexeme)
    }

    @Test
    fun `Tokenise punctuation`() {
        val tokens = "()[]{}".tokenizeSML()
        assertEquals(6, tokens.size)
        assertEquals(SMLTokenType.Punctuation.LPAREN, tokens[0].type)
        assertEquals(SMLTokenType.Punctuation.RPAREN, tokens[1].type)
        assertEquals(SMLTokenType.Punctuation.LBRACK, tokens[2].type)
        assertEquals(SMLTokenType.Punctuation.RBRACK, tokens[3].type)
        assertEquals(SMLTokenType.Punctuation.LBRACE, tokens[4].type)
        assertEquals(SMLTokenType.Punctuation.RBRACE, tokens[5].type)
    }

    @Test
    fun `Tokenise double arrow`() {
        val tokens = "=>".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Punctuation.DOUBLE_ARROW, tokens[0].type)
    }

    @Test
    fun `Tokenise arrow`() {
        val tokens = "->".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Punctuation.ARROW, tokens[0].type)
    }

    @Test
    fun `Tokenise ellipsis`() {
        val tokens = "...".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Punctuation.ELLIPSIS, tokens[0].type)
    }

    @Test
    fun `Tokenise case expression`() {
        val tokens = "case x of 0 => 1 | _ => 2".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(SMLTokenType.ReservedWord.CASE, withoutWs[0].type)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[1].type)
        assertEquals(SMLTokenType.ReservedWord.OF, withoutWs[2].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[3].type)
        assertEquals(SMLTokenType.Punctuation.DOUBLE_ARROW, withoutWs[4].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[5].type)
        assertEquals(SMLTokenType.Punctuation.PIPE, withoutWs[6].type)
        assertEquals(SMLTokenType.Punctuation.UNDERBAR, withoutWs[7].type)
        assertEquals(SMLTokenType.Punctuation.DOUBLE_ARROW, withoutWs[8].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[9].type)
    }

    @Test
    fun `Tokenise let expression`() {
        val tokens = "let val x = 1 in x end".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(SMLTokenType.ReservedWord.LET, withoutWs[0].type)
        assertEquals(SMLTokenType.ReservedWord.VAL, withoutWs[1].type)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[2].type)
        assertEquals(SMLTokenType.Punctuation.EQUALS, withoutWs[3].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[4].type)
        assertEquals(SMLTokenType.ReservedWord.IN, withoutWs[5].type)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[6].type)
        assertEquals(SMLTokenType.ReservedWord.END, withoutWs[7].type)
    }

    @Test
    fun `Tokenise datatype declaration`() {
        val tokens = "datatype 'a list = nil | cons of 'a".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(SMLTokenType.ReservedWord.DATATYPE, withoutWs[0].type)
        assertEquals(SMLTokenType.Identifier.TYVAR, withoutWs[1].type)
        assertEquals("'a", withoutWs[1].lexeme)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[2].type)
        assertEquals("list", withoutWs[2].lexeme)
    }

    @Test
    fun `Token positions are tracked correctly`() {
        val tokens = "val x".tokenizeSML()

        assertEquals(0, tokens[0].position)
        assertEquals(3, tokens[1].position)
        assertEquals(4, tokens[2].position)
    }

    @Test
    fun `Token compact string format`() {
        val token = Token(SMLTokenType.ReservedWord.VAL, "val", 0)
        assertEquals("val(val)", token.toCompactString())
    }

    @Test
    fun `Token sequence compact string format`() {
        val tokens = "val x".tokenizeSML()
        assertEquals("val(val) whitespace( ) alphanumeric_id(x)", tokens.toCompactString())
    }

    @Test
    fun `Tokenise hash followed by digit`() {
        val tokens = "#1".tokenizeSML()
        assertEquals(2, tokens.size)
        assertEquals(SMLTokenType.Punctuation.HASH, tokens[0].type)
        assertEquals(SMLTokenType.Literal.INTEGER, tokens[1].type)
        assertEquals("1", tokens[1].lexeme)
    }

    @Test
    fun `Tokenise numeric label pattern`() {
        val tokens = "#2 r".tokenizeSML()
        val withoutWs = tokens.withoutTrivia()

        assertEquals(3, withoutWs.size)
        assertEquals(SMLTokenType.Punctuation.HASH, withoutWs[0].type)
        assertEquals(SMLTokenType.Literal.INTEGER, withoutWs[1].type)
        assertEquals("2", withoutWs[1].lexeme)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, withoutWs[2].type)
    }

    @Test
    fun `Tokenise handles newlines`() {
        val tokens = "val\nx".tokenizeSML()

        assertEquals(3, tokens.size)
        assertEquals(SMLTokenType.ReservedWord.VAL, tokens[0].type)
        assertEquals(SMLTokenType.Trivia.NEWLINE, tokens[1].type)
        assertEquals("\n", tokens[1].lexeme)
        assertEquals(SMLTokenType.Identifier.ALPHANUMERIC_ID, tokens[2].type)
    }

    @Test
    fun `TokeniseSMLOrNull returns null on invalid input`() {
        val result = "val x = §".tokenizeSMLOrNull()
        assertNull(result)
    }

    @Test
    fun `Lexical error provides context`() {
        val error =
            assertThrows<TokenisationError> {
                "val x = §invalid".tokenizeSML()
            }
        assertTrue(error.context.contains("§"))
    }

    @Test
    fun `At (@) symbol is valid symbolic identifier`() {
        val tokens = "@@@".tokenizeSML()
        assertEquals(1, tokens.size)
        assertEquals(SMLTokenType.Identifier.SYMBOLIC_ID, tokens[0].type)
        assertEquals("@@@", tokens[0].lexeme)
    }

    @Test
    fun `All reserved words are recognized`() {
        val reservedWords =
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

        for (word in reservedWords) {
            val tokens = word.tokenizeSML()
            assertEquals(1, tokens.size, "Expected 1 token for reserved word: $word")
            assertTrue(tokens[0].type is SMLTokenType.ReservedWord, "Expected ReservedWord for: $word")
            assertEquals(word, tokens[0].lexeme)
        }
    }

    @Test
    fun `withoutTrivia filters correctly`() {
        val tokens = "  val   x  ".tokenizeSML()
        val withoutTrivia = tokens.withoutTrivia()

        assertEquals(2, withoutTrivia.size)
        assertTrue(withoutTrivia.none { it.type is SMLTokenType.Trivia })
    }
}
