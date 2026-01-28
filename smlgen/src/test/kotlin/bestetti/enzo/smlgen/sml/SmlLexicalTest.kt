package bestetti.enzo.smlgen.sml

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import bestetti.enzo.smlgen.sml.grammar.SmlLexical
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class SmlLexicalTest {

    private fun ctx(seed: Long = 42L) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = 5, maxRepeat = 3)
    )

    @Test
    fun `alphanumeric identifier starts with letter`() {
        repeat(50) { i ->
            val id = SmlLexical.alphanumId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "ID should not be empty")
            assertTrue(id[0].isLetter(), "ID should start with letter: $id")
        }
    }

    @Test
    fun `alphanumeric identifier contains only valid characters`() {
        val validChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('_', '\'')
        repeat(50) { i ->
            val id = SmlLexical.alphanumId(ctx(i.toLong()))
            assertTrue(id.all { it in validChars }, "Invalid char in ID: $id")
        }
    }

    @Test
    fun `value identifier avoids reserved words`() {
        val reserved = setOf(
            "abstype", "and", "andalso", "as", "case", "datatype", "do", "else",
            "end", "exception", "fn", "fun", "handle", "if", "in", "infix",
            "infixr", "let", "local", "nonfix", "of", "op", "open", "orelse",
            "raise", "rec", "then", "type", "val", "where", "while", "with", "withtype"
        )
        repeat(100) { i ->
            val id = SmlLexical.valueId(ctx(i.toLong()))
            assertFalse(id in reserved, "Generated reserved word: $id")
        }
    }

    @Test
    fun `symbolic identifier contains only symbolic characters`() {
        val symbolicChars =
            setOf('!', '%', '&', '$', '#', '+', '-', '/', ':', '<', '=', '>', '?', '@', '\\', '~', '`', '^', '|', '*')
        repeat(50) { i ->
            val id = SmlLexical.symbolicId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Symbolic ID should not be empty")
            assertTrue(id.all { it in symbolicChars }, "Invalid symbolic char in: $id")
        }
    }

    @Test
    fun `type variable starts with prime`() {
        repeat(50) { i ->
            val tv = SmlLexical.typeVar(ctx(i.toLong()))
            assertTrue(tv.startsWith("'"), "Type var should start with ': $tv")
        }
    }

    @Test
    fun `equality type variable starts with two primes`() {
        repeat(50) { i ->
            val tv = SmlLexical.eqTypeVar(ctx(i.toLong()))
            assertTrue(tv.startsWith("''"), "Eq type var should start with '': $tv")
        }
    }

    @Test
    fun `decimal integer is valid`() {
        repeat(50) { i ->
            val num = SmlLexical.decimalInt(ctx(i.toLong()))
            // Should match SML integer format (optional ~ for negative)
            assertTrue(num.matches(Regex("~?[0-9]+")), "Invalid decimal int: $num")
        }
    }

    @Test
    fun `hex integer is valid`() {
        repeat(50) { i ->
            val num = SmlLexical.hexInt(ctx(i.toLong()))
            assertTrue(num.matches(Regex("~?0x[0-9a-fA-F]+")), "Invalid hex int: $num")
        }
    }

    @Test
    fun `word constant is valid`() {
        repeat(50) { i ->
            val w = SmlLexical.wordConst(ctx(i.toLong()))
            assertTrue(
                w.matches(Regex("0w[0-9]+")) || w.matches(Regex("0wx[0-9a-fA-F]+")),
                "Invalid word: $w"
            )
        }
    }

    @Test
    fun `real constant has decimal or exponent`() {
        repeat(50) { i ->
            val r = SmlLexical.realConst(ctx(i.toLong()))
            assertTrue(
                r.contains('.') || r.contains('e') || r.contains('E'),
                "Real should have decimal or exponent: $r"
            )
        }
    }

    @Test
    fun `string constant is properly quoted`() {
        repeat(50) { i ->
            val s = SmlLexical.stringConst(ctx(i.toLong()))
            assertTrue(s.startsWith('"'), "String should start with quote: $s")
            assertTrue(s.endsWith('"'), "String should end with quote: $s")
        }
    }

    @Test
    fun `character constant is properly formatted`() {
        repeat(50) { i ->
            val c = SmlLexical.charConst(ctx(i.toLong()))
            assertTrue(c.startsWith("#\""), "Char should start with #\": $c")
            assertTrue(c.endsWith('"'), "Char should end with \": $c")
        }
    }

    @Test
    fun `comment does not contain nested comment markers`() {
        repeat(100) { i ->
            val comment = SmlLexical.comment(ctx(i.toLong()))
            assertTrue(comment.startsWith("(*"), "Comment should start with (*")
            assertTrue(comment.endsWith("*)"), "Comment should end with *)")

            // Check no nested (* or *) in the content
            val content = comment.drop(2).dropLast(2)
            assertFalse(content.contains("(*"), "Nested (* found in: $comment")
            assertFalse(content.contains("*)"), "Nested *) found in: $comment")
        }
    }

    @Test
    fun `struct identifier starts with uppercase`() {
        repeat(50) { i ->
            val id = SmlLexical.structId(ctx(i.toLong()))
            assertTrue(id[0].isUpperCase(), "Struct ID should start uppercase: $id")
        }
    }

    @Test
    fun `label is valid alphanumeric or numeric`() {
        repeat(50) { i ->
            val label = SmlLexical.label(ctx(i.toLong()))
            assertTrue(
                label[0].isLetter() || label[0].isDigit(),
                "Label should start with letter or digit: $label"
            )
        }
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlLexical.alphanumId(ctx(12345L))
        val result2 = SmlLexical.alphanumId(ctx(12345L))
        assertEquals(result1, result2, "Same seed should produce same result")
    }

    @Test
    fun `generation varies with different seeds`() {
        val results = (0L until 10L).map { SmlLexical.alphanumId(ctx(it)) }.toSet()
        assertTrue(results.size > 1, "Different seeds should produce different results")
    }
}
