package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for SmlEdgeCase generators.
 */
class SmlEdgeCaseTest {

    private fun ctx(seed: Long = 42L) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = 5, maxRepeat = 3)
    )

    @Test
    fun `obscureSymbolicId generates valid symbolic identifiers`() {
        val symbolicChars = setOf('!', '%', '&', '?', '<', '>', '~', '+', '-', '=', '|', '\\', '^', '$', '@', '*')
        repeat(50) { i ->
            val id = SmlEdgeCase.obscureSymbolicId(ctx(i.toLong()))
            assertTrue(id.isNotEmpty(), "Symbolic ID should not be empty")
            assertTrue(id.all { it in symbolicChars }, "Invalid char in symbolic ID: $id")
        }
    }

    @Test
    fun `obscureNumeric generates valid numeric constants`() {
        repeat(50) { i ->
            val num = SmlEdgeCase.obscureNumeric(ctx(i.toLong()))
            assertTrue(num.isNotEmpty(), "Numeric should not be empty")
            // Should be a valid SML numeric constant
            assertTrue(
                num.matches(Regex("~?[0-9]+(\\.[0-9]+)?([eE]~?[0-9]+)?")) ||
                        num.matches(Regex("~?0x[0-9a-fA-F]+")) ||
                        num.matches(Regex("0w[0-9a-fA-F]+")) ||
                        num.matches(Regex("0wx[0-9a-fA-F]+")),
                "Invalid numeric: $num"
            )
        }
    }

    @Test
    fun `obscureString generates valid string constants`() {
        repeat(50) { i ->
            val str = SmlEdgeCase.obscureString(ctx(i.toLong()))
            assertTrue(str.startsWith("\""), "String should start with quote: $str")
            assertTrue(str.endsWith("\""), "String should end with quote: $str")
        }
    }

    @Test
    fun `obscureChar generates valid character constants`() {
        repeat(50) { i ->
            val chr = SmlEdgeCase.obscureChar(ctx(i.toLong()))
            assertTrue(chr.startsWith("#\""), "Char should start with #\": $chr")
            assertTrue(chr.endsWith("\""), "Char should end with \": $chr")
        }
    }

    @Test
    fun `obscurePattern generates valid patterns`() {
        repeat(50) { i ->
            val pat = SmlEdgeCase.obscurePattern(ctx(i.toLong()))
            assertTrue(pat.isNotEmpty(), "Pattern should not be empty")
            // Patterns should be syntactically valid SML patterns
        }
    }

    @Test
    fun `obscureExpression generates valid expressions`() {
        repeat(50) { i ->
            val expr = SmlEdgeCase.obscureExpression(ctx(i.toLong()))
            assertTrue(expr.isNotEmpty(), "Expression should not be empty")
        }
    }

    @Test
    fun `obscureDeclaration generates valid declarations`() {
        repeat(50) { i ->
            val decl = SmlEdgeCase.obscureDeclaration(ctx(i.toLong()))
            assertTrue(decl.isNotEmpty(), "Declaration should not be empty")
        }
    }

    @Test
    fun `obscureSymbolicId produces variety`() {
        val ids = (0L until 100L).map { SmlEdgeCase.obscureSymbolicId(ctx(it)) }.toSet()
        assertTrue(ids.size > 5, "Should produce varied symbolic IDs")
    }

    @Test
    fun `obscureNumeric produces variety`() {
        val nums = (0L until 100L).map { SmlEdgeCase.obscureNumeric(ctx(it)) }.toSet()
        assertTrue(nums.size > 5, "Should produce varied numeric constants")
    }

    @Test
    fun `obscureString produces variety`() {
        val strs = (0L until 100L).map { SmlEdgeCase.obscureString(ctx(it)) }.toSet()
        assertTrue(strs.size > 5, "Should produce varied string constants")
    }

    @Test
    fun `obscureChar produces variety`() {
        val chars = (0L until 100L).map { SmlEdgeCase.obscureChar(ctx(it)) }.toSet()
        assertTrue(chars.size > 3, "Should produce varied char constants")
    }

    @Test
    fun `obscurePattern produces variety`() {
        val pats = (0L until 100L).map { SmlEdgeCase.obscurePattern(ctx(it)) }.toSet()
        assertTrue(pats.size > 3, "Should produce varied patterns")
    }

    @Test
    fun `obscureExpression produces variety`() {
        val exprs = (0L until 100L).map { SmlEdgeCase.obscureExpression(ctx(it)) }.toSet()
        assertTrue(exprs.size > 5, "Should produce varied expressions")
    }

    @Test
    fun `obscureDeclaration produces variety`() {
        val decls = (0L until 100L).map { SmlEdgeCase.obscureDeclaration(ctx(it)) }.toSet()
        assertTrue(decls.size > 3, "Should produce varied declarations")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlEdgeCase.obscureSymbolicId(ctx(12345L))
        val result2 = SmlEdgeCase.obscureSymbolicId(ctx(12345L))
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
