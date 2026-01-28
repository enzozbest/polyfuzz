package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for SmlLexical generators with focus on edge cases and full branch coverage.
 */
class SmlLexicalTest {

    private fun ctx(seed: Long = 42L, maxDepth: Int = 5, maxRepeat: Int = 3) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = maxDepth, maxRepeat = maxRepeat)
    )

    @Test
    fun `alphanumId generates valid identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.alphanumId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "ID should not be empty")
            assertTrue(id[0].isLetter(), "ID should start with letter: $id")
        }
    }

    @Test
    fun `shortAlphanumId generates short identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.shortAlphanumId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "ID should not be empty")
            assertTrue(id.length <= 4, "Short ID should be at most 4 chars: $id")
        }
    }

    @Test
    fun `symbolicId generates valid symbolic identifiers`() {
        val symbolicChars =
            setOf('!', '%', '&', '$', '#', '+', '-', '/', ':', '<', '=', '>', '?', '@', '\\', '~', '`', '^', '|', '*')
        repeat(50) { i ->
            val id = SmlLexical.symbolicId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Symbolic ID should not be empty")
            assertTrue(id.all { it in symbolicChars }, "Invalid char in symbolic ID: $id")
            assertFalse(id == "#" || id == "|", "Should not generate reserved single chars: $id")
        }
    }

    @Test
    fun `typeVar generates type variables`() {
        repeat(50) { i ->
            val tv = SmlLexical.typeVar(ctx(i.toLong()))
            assertTrue(tv.startsWith("'"), "Type var should start with ': $tv")
        }
    }

    @Test
    fun `eqTypeVar generates equality type variables`() {
        repeat(50) { i ->
            val tv = SmlLexical.eqTypeVar(ctx(i.toLong()))
            assertTrue(tv.startsWith("''"), "Eq type var should start with '': $tv")
        }
    }

    @Test
    fun `longValueId generates long value identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.longValueId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Long value ID should not be empty")
        }
    }

    @Test
    fun `longTyconId generates long type constructor identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.longTyconId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Long tycon ID should not be empty")
        }
    }

    @Test
    fun `comment generates valid comments`() {
        repeat(50) { i ->
            val comment = SmlLexical.comment(ctx(i.toLong()))
            assertTrue(comment.startsWith("(*"), "Comment should start with (*: $comment")
            assertTrue(comment.endsWith("*)"), "Comment should end with *): $comment")
            // Verify no nested comment markers
            val content = comment.drop(2).dropLast(2)
            assertFalse(content.contains("(*"), "Comment should not contain nested (*: $comment")
            assertFalse(content.contains("*)"), "Comment should not contain nested *): $comment")
        }
    }

    @Test
    fun `optComment generates optional comments`() {
        var foundEmpty = false
        var foundComment = false
        repeat(100) { i ->
            val result = SmlLexical.optComment(ctx(i.toLong()))
            if (result.isEmpty()) foundEmpty = true
            if (result.startsWith("(*")) foundComment = true
        }
        assertTrue(foundEmpty, "Should sometimes generate empty")
        assertTrue(foundComment, "Should sometimes generate comment")
    }

    @Test
    fun `intConst generates valid integers`() {
        repeat(50) { i ->
            val num = SmlLexical.intConst(ctx(i.toLong()))
            assertTrue(num.isNotEmpty(), "Integer should not be empty")
            // Valid patterns: digits, ~digits, 0xhex, ~0xhex
            assertTrue(
                num.matches(Regex("~?[0-9]+")) || num.matches(Regex("~?0x[0-9a-fA-F]+")),
                "Invalid integer: $num"
            )
        }
    }

    @Test
    fun `wordConst generates valid word constants`() {
        repeat(50) { i ->
            val word = SmlLexical.wordConst(ctx(i.toLong()))
            assertTrue(word.isNotEmpty(), "Word should not be empty")
            assertTrue(word.startsWith("0w"), "Word should start with 0w: $word")
        }
    }

    @Test
    fun `realConst generates valid real numbers`() {
        repeat(50) { i ->
            val real = SmlLexical.realConst(ctx(i.toLong()))
            assertTrue(real.isNotEmpty(), "Real should not be empty")
        }
    }

    @Test
    fun `stringConst generates valid strings`() {
        repeat(50) { i ->
            val str = SmlLexical.stringConst(ctx(i.toLong()))
            assertTrue(str.startsWith("\""), "String should start with \": $str")
            assertTrue(str.endsWith("\""), "String should end with \": $str")
        }
    }

    @Test
    fun `charConst generates valid char constants`() {
        repeat(50) { i ->
            val chr = SmlLexical.charConst(ctx(i.toLong()))
            assertTrue(chr.startsWith("#\""), "Char should start with #\": $chr")
            assertTrue(chr.endsWith("\""), "Char should end with \": $chr")
        }
    }

    @Test
    fun `constant generates various constants`() {
        val constants = (0L until 100L).map { SmlLexical.constant(ctx(it)) }.toSet()
        assertTrue(constants.size > 20, "Should produce varied constants")
    }

    @Test
    fun `space generates whitespace`() {
        val space = SmlLexical.space(ctx())
        assertEquals(" ", space, "Space should be a single space")
    }

    @Test
    fun `comment content handles edge cases - generates many comments`() {
        // Run many iterations to hit edge case branches
        // This tests lines 65-78 for stripping leading/trailing problematic chars
        for (seed in 0L until 10000L) {
            val comment = SmlLexical.comment(ctx(seed))
            // Verify comment is valid (no nested markers)
            assertTrue(comment.startsWith("(*"), "Comment should start with (*")
            assertTrue(comment.endsWith("*)"), "Comment should end with *)")
            val content = comment.drop(2).dropLast(2)
            assertFalse(content.contains("(*"), "Content should not contain (*")
            assertFalse(content.contains("*)"), "Content should not contain *)")
        }
    }

    @Test
    fun `symbolicId avoids reserved single chars`() {
        // Test that single # and | are avoided
        val ids = (0L until 1000L).map { SmlLexical.symbolicId(ctx(it)) }
        assertFalse(ids.contains("#"), "Should not generate single #")
        assertFalse(ids.contains("|"), "Should not generate single |")
    }

    @Test
    fun `intConst generates negative numbers`() {
        var foundNegative = false
        repeat(100) { i ->
            val num = SmlLexical.intConst(ctx(i.toLong()))
            if (num.startsWith("~")) foundNegative = true
        }
        assertTrue(foundNegative, "Should generate some negative integers")
    }

    @Test
    fun `intConst generates hex numbers`() {
        var foundHex = false
        repeat(100) { i ->
            val num = SmlLexical.intConst(ctx(i.toLong()))
            if (num.contains("0x")) foundHex = true
        }
        assertTrue(foundHex, "Should generate some hex integers")
    }

    @Test
    fun `realConst generates numbers with exponents`() {
        var foundExponent = false
        repeat(100) { i ->
            val num = SmlLexical.realConst(ctx(i.toLong()))
            if (num.contains("e") || num.contains("E")) foundExponent = true
        }
        assertTrue(foundExponent, "Should generate some reals with exponents")
    }

    @Test
    fun `stringConst generates escape sequences`() {
        val strings = (0L until 100L).map { SmlLexical.stringConst(ctx(it)) }
        val hasEscapes = strings.any { it.contains("\\") }
        assertTrue(hasEscapes, "Should generate some strings with escape sequences")
    }

    @Test
    fun `valueId generates valid value identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.valueId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Value ID should not be empty")
        }
    }

    @Test
    fun `structId generates struct identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.structId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Struct ID should not be empty")
            assertTrue(id[0].isUpperCase(), "Struct ID should start with uppercase: $id")
        }
    }

    @Test
    fun `label generates valid labels`() {
        repeat(50) { i ->
            val label = SmlLexical.label(ctx(i.toLong()))
            assertTrue(label.isNotEmpty(), "Label should not be empty")
        }
    }

    @Test
    fun `anyTypeVar generates type variables`() {
        repeat(50) { i ->
            val tv = SmlLexical.anyTypeVar(ctx(i.toLong()))
            assertTrue(tv.startsWith("'"), "Any type var should start with ': $tv")
        }
    }

    @Test
    fun `builtinValueId generates builtin identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.builtinValueId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Builtin value ID should not be empty")
        }
    }

    @Test
    fun `builtinTypeId generates builtin type identifiers`() {
        repeat(50) { i ->
            val id = SmlLexical.builtinTypeId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Builtin type ID should not be empty")
        }
    }

    @Test
    fun `infixOp generates infix operators`() {
        repeat(50) { i ->
            val op = SmlLexical.infixOp(ctx(i.toLong()))
            assertTrue(op.isNotEmpty(), "Infix op should not be empty")
        }
    }

    @Test
    fun `prefixOp generates prefix operators`() {
        repeat(50) { i ->
            val op = SmlLexical.prefixOp(ctx(i.toLong()))
            assertTrue(op.isNotEmpty(), "Prefix op should not be empty")
        }
    }

    @Test
    fun `patternConstant generates pattern constants`() {
        repeat(50) { i ->
            val c = SmlLexical.patternConstant(ctx(i.toLong()))
            assertTrue(c.isNotEmpty(), "Pattern constant should not be empty")
        }
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlLexical.alphanumId(ctx(12345L))
        val result2 = SmlLexical.alphanumId(ctx(12345L))
        assertEquals(result1, result2, "Same seed should produce same result")
    }
}
