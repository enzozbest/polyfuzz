@file:Suppress("ktlint:standard:no-wildcard-imports")

package tokenizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SMLTokenTest {
    @Test
    fun testSMLTokenTypeFromTagReservedWords() {
        assertEquals(SMLTokenType.ReservedWord.ABSTYPE, SMLTokenType.fromTag("ABSTYPE"))
        assertEquals(SMLTokenType.ReservedWord.AND, SMLTokenType.fromTag("AND"))
        assertEquals(SMLTokenType.ReservedWord.ANDALSO, SMLTokenType.fromTag("ANDALSO"))
        assertEquals(SMLTokenType.ReservedWord.AS, SMLTokenType.fromTag("AS"))
        assertEquals(SMLTokenType.ReservedWord.CASE, SMLTokenType.fromTag("CASE"))
        assertEquals(SMLTokenType.ReservedWord.DATATYPE, SMLTokenType.fromTag("DATATYPE"))
        assertEquals(SMLTokenType.ReservedWord.DO, SMLTokenType.fromTag("DO"))
        assertEquals(SMLTokenType.ReservedWord.ELSE, SMLTokenType.fromTag("ELSE"))
        assertEquals(SMLTokenType.ReservedWord.END, SMLTokenType.fromTag("END"))
        assertEquals(SMLTokenType.ReservedWord.EXCEPTION, SMLTokenType.fromTag("EXCEPTION"))
        assertEquals(SMLTokenType.ReservedWord.FN, SMLTokenType.fromTag("FN"))
        assertEquals(SMLTokenType.ReservedWord.FUN, SMLTokenType.fromTag("FUN"))
        assertEquals(SMLTokenType.ReservedWord.HANDLE, SMLTokenType.fromTag("HANDLE"))
        assertEquals(SMLTokenType.ReservedWord.IF, SMLTokenType.fromTag("IF"))
        assertEquals(SMLTokenType.ReservedWord.IN, SMLTokenType.fromTag("IN"))
        assertEquals(SMLTokenType.ReservedWord.INFIX, SMLTokenType.fromTag("INFIX"))
        assertEquals(SMLTokenType.ReservedWord.INFIXR, SMLTokenType.fromTag("INFIXR"))
        assertEquals(SMLTokenType.ReservedWord.LET, SMLTokenType.fromTag("LET"))
        assertEquals(SMLTokenType.ReservedWord.LOCAL, SMLTokenType.fromTag("LOCAL"))
        assertEquals(SMLTokenType.ReservedWord.NONFIX, SMLTokenType.fromTag("NONFIX"))
        assertEquals(SMLTokenType.ReservedWord.OF, SMLTokenType.fromTag("OF"))
        assertEquals(SMLTokenType.ReservedWord.OP, SMLTokenType.fromTag("OP"))
        assertEquals(SMLTokenType.ReservedWord.OPEN, SMLTokenType.fromTag("OPEN"))
        assertEquals(SMLTokenType.ReservedWord.ORELSE, SMLTokenType.fromTag("ORELSE"))
        assertEquals(SMLTokenType.ReservedWord.RAISE, SMLTokenType.fromTag("RAISE"))
        assertEquals(SMLTokenType.ReservedWord.REC, SMLTokenType.fromTag("REC"))
        assertEquals(SMLTokenType.ReservedWord.THEN, SMLTokenType.fromTag("THEN"))
        assertEquals(SMLTokenType.ReservedWord.TYPE, SMLTokenType.fromTag("TYPE"))
        assertEquals(SMLTokenType.ReservedWord.VAL, SMLTokenType.fromTag("VAL"))
        assertEquals(SMLTokenType.ReservedWord.WITH, SMLTokenType.fromTag("WITH"))
        assertEquals(SMLTokenType.ReservedWord.WITHTYPE, SMLTokenType.fromTag("WITHTYPE"))
        assertEquals(SMLTokenType.ReservedWord.WHILE, SMLTokenType.fromTag("WHILE"))
    }

    @Test
    fun testSMLTokenTypeFromTagPunctuation() {
        assertEquals(SMLTokenType.Punctuation.LPAREN, SMLTokenType.fromTag("("))
        assertEquals(SMLTokenType.Punctuation.RPAREN, SMLTokenType.fromTag(")"))
        assertEquals(SMLTokenType.Punctuation.LBRACK, SMLTokenType.fromTag("["))
        assertEquals(SMLTokenType.Punctuation.RBRACK, SMLTokenType.fromTag("]"))
        assertEquals(SMLTokenType.Punctuation.LBRACE, SMLTokenType.fromTag("{"))
        assertEquals(SMLTokenType.Punctuation.RBRACE, SMLTokenType.fromTag("}"))
        assertEquals(SMLTokenType.Punctuation.COMMA, SMLTokenType.fromTag(","))
        assertEquals(SMLTokenType.Punctuation.COLON, SMLTokenType.fromTag(":"))
        assertEquals(SMLTokenType.Punctuation.SEMICOLON, SMLTokenType.fromTag(";"))
        assertEquals(SMLTokenType.Punctuation.ELLIPSIS, SMLTokenType.fromTag("..."))
        assertEquals(SMLTokenType.Punctuation.UNDERBAR, SMLTokenType.fromTag("_"))
        assertEquals(SMLTokenType.Punctuation.PIPE, SMLTokenType.fromTag("|"))
        assertEquals(SMLTokenType.Punctuation.EQUALS, SMLTokenType.fromTag("="))
        assertEquals(SMLTokenType.Punctuation.DOUBLE_ARROW, SMLTokenType.fromTag("=>"))
        assertEquals(SMLTokenType.Punctuation.ARROW, SMLTokenType.fromTag("->"))
        assertEquals(SMLTokenType.Punctuation.HASH, SMLTokenType.fromTag("#"))
    }

    @Test
    fun testSMLTokenTypeFromTagLiterals() {
        assertEquals(SMLTokenType.Literal.INTEGER, SMLTokenType.fromTag("INT"))
        assertEquals(SMLTokenType.Literal.WORD, SMLTokenType.fromTag("WORD"))
        assertEquals(SMLTokenType.Literal.REAL, SMLTokenType.fromTag("REAL"))
        assertEquals(SMLTokenType.Literal.STRING, SMLTokenType.fromTag("STRING"))
        assertEquals(SMLTokenType.Literal.CHAR, SMLTokenType.fromTag("CHAR"))
    }

    @Test
    fun testSMLTokenTypeFromTagIdentifiers() {
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, SMLTokenType.fromTag("ID"))
        // Both TYPE_IDENTIFIER and ETYPE_IDENTIFIER have displayName "TYID"
        // The later one (ETYPE_IDENTIFIER) wins in the map
        assertEquals(SMLTokenType.Identifier.ETYPE_IDENTIFIER, SMLTokenType.fromTag("TYID"))
        assertEquals(SMLTokenType.Identifier.NUMERIC_LABEL, SMLTokenType.fromTag("numeric_label"))
    }

    @Test
    fun testSMLTokenTypeFromTagTrivia() {
        assertEquals(SMLTokenType.Trivia.WHITESPACE, SMLTokenType.fromTag("WS"))
        assertEquals(SMLTokenType.Trivia.NEWLINE, SMLTokenType.fromTag("NL"))
        assertEquals(SMLTokenType.Trivia.COMMENT, SMLTokenType.fromTag("C"))
    }

    @Test
    fun testSMLTokenTypeFromTagUnknown() {
        assertThrows(IllegalStateException::class.java) {
            SMLTokenType.fromTag("UNKNOWN_TAG")
        }
    }

    @Test
    fun testReservedWordFromLexeme() {
        assertEquals(SMLTokenType.ReservedWord.ABSTYPE, SMLTokenType.ReservedWord.fromLexeme("ABSTYPE"))
        assertEquals(SMLTokenType.ReservedWord.IF, SMLTokenType.ReservedWord.fromLexeme("IF"))
        assertEquals(SMLTokenType.ReservedWord.WHILE, SMLTokenType.ReservedWord.fromLexeme("WHILE"))
        assertNull(SMLTokenType.ReservedWord.fromLexeme("notareservedword"))
        assertNull(SMLTokenType.ReservedWord.fromLexeme(""))
    }

    @Test
    fun testTokenProperties() {
        val token = Token(SMLTokenType.ReservedWord.IF, "if", 10)

        assertEquals(SMLTokenType.ReservedWord.IF, token.type)
        assertEquals("if", token.lexeme)
        assertEquals(10, token.position)
        assertEquals(2, token.length)
        assertEquals(12, token.endPosition)
    }

    @Test
    fun testTokenToString() {
        val token = Token(SMLTokenType.ReservedWord.IF, "if", 0)
        assertEquals("Token(IF, \"if\", pos=0)", token.toString())
    }

    @Test
    fun testTokenToCompactStringLiteral() {
        val intToken = Token(SMLTokenType.Literal.INTEGER, "42", 0)
        assertEquals("INT(42)", intToken.toCompactString())

        val stringToken = Token(SMLTokenType.Literal.STRING, "\"hello\"", 0)
        assertEquals("STRING(\"hello\")", stringToken.toCompactString())
    }

    @Test
    fun testTokenToCompactStringIdentifier() {
        val idToken = Token(SMLTokenType.Identifier.IDENTIFIER, "myVar", 0)
        assertEquals("ID(myVar)", idToken.toCompactString())

        val tyidToken = Token(SMLTokenType.Identifier.TYPE_IDENTIFIER, "'a", 0)
        assertEquals("TYID('a)", tyidToken.toCompactString())
    }

    @Test
    fun testTokenToCompactStringOther() {
        val ifToken = Token(SMLTokenType.ReservedWord.IF, "if", 0)
        assertEquals("IF", ifToken.toCompactString())

        val parenToken = Token(SMLTokenType.Punctuation.LPAREN, "(", 0)
        assertEquals("(", parenToken.toCompactString())

        val wsToken = Token(SMLTokenType.Trivia.WHITESPACE, " ", 0)
        assertEquals("WS", wsToken.toCompactString())
    }

    @Test
    fun testTokenFromPair() {
        val token = Token.fromPair("IF" to "if", 5)
        assertEquals(SMLTokenType.ReservedWord.IF, token.type)
        assertEquals("if", token.lexeme)
        assertEquals(5, token.position)
    }

    @Test
    fun testTokenSequenceWithoutTrivia() {
        val tokens = listOf(
            Token(SMLTokenType.ReservedWord.IF, "if", 0),
            Token(SMLTokenType.Trivia.WHITESPACE, " ", 2),
            Token(SMLTokenType.Identifier.IDENTIFIER, "x", 3),
            Token(SMLTokenType.Trivia.NEWLINE, "\n", 4),
            Token(SMLTokenType.ReservedWord.THEN, "then", 5)
        )
        val seq = TokenSequence(tokens)

        val withoutTrivia = seq.withoutTrivia()
        assertEquals(3, withoutTrivia.size)
        assertEquals(SMLTokenType.ReservedWord.IF, withoutTrivia[0].type)
        assertEquals(SMLTokenType.Identifier.IDENTIFIER, withoutTrivia[1].type)
        assertEquals(SMLTokenType.ReservedWord.THEN, withoutTrivia[2].type)
    }

    @Test
    fun testTokenSequenceToCompactString() {
        val tokens = listOf(
            Token(SMLTokenType.ReservedWord.IF, "if", 0),
            Token(SMLTokenType.Identifier.IDENTIFIER, "x", 3)
        )
        val seq = TokenSequence(tokens)

        assertEquals("IF ID(x)", seq.toCompactString())
    }

    @Test
    fun testTokenSequenceToDetailedString() {
        val tokens = listOf(
            Token(SMLTokenType.ReservedWord.IF, "if", 0),
            Token(SMLTokenType.Identifier.IDENTIFIER, "x", 3)
        )
        val seq = TokenSequence(tokens)

        val detailed = seq.toDetailedString()
        assertTrue(detailed.contains("Token(IF"))
        assertTrue(detailed.contains("Token(ID"))
        assertTrue(detailed.contains("pos=0"))
        assertTrue(detailed.contains("pos=3"))
    }

    @Test
    fun testTokenSequenceDelegation() {
        val tokens = listOf(
            Token(SMLTokenType.ReservedWord.IF, "if", 0),
            Token(SMLTokenType.Identifier.IDENTIFIER, "x", 3)
        )
        val seq = TokenSequence(tokens)

        assertEquals(2, seq.size)
        assertEquals(tokens[0], seq[0])
        assertEquals(tokens[1], seq[1])
        assertTrue(seq.contains(tokens[0]))
        assertFalse(seq.isEmpty())
    }

    @Test
    fun testDisplayNames() {
        assertEquals("ABSTYPE", SMLTokenType.ReservedWord.ABSTYPE.displayName)
        assertEquals("(", SMLTokenType.Punctuation.LPAREN.displayName)
        assertEquals("INT", SMLTokenType.Literal.INTEGER.displayName)
        assertEquals("ID", SMLTokenType.Identifier.IDENTIFIER.displayName)
        assertEquals("WS", SMLTokenType.Trivia.WHITESPACE.displayName)
    }
}
