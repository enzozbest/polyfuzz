package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SmlPatterns generators.
 */
class SmlPatternsTest {

    private fun ctx(seed: Long = 42L, maxDepth: Int = 5, maxRepeat: Int = 3) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = maxDepth, maxRepeat = maxRepeat)
    )

    @Test
    fun `wildcard generates underscore`() {
        val pat = SmlPatterns.wildcard(ctx())
        assertEquals("_", pat, "Wildcard should be '_'")
    }

    @Test
    fun `varPat generates variable patterns`() {
        repeat(50) { i ->
            val pat = SmlPatterns.varPat(ctx(i.toLong()))
            assertTrue(pat.isNotEmpty(), "Var pattern should not be empty")
            assertTrue(pat[0].isLetter(), "Var pattern should start with letter: $pat")
        }
    }

    @Test
    fun `constPat generates constant patterns`() {
        repeat(50) { i ->
            val pat = SmlPatterns.constPat(ctx(i.toLong()))
            assertTrue(pat.isNotEmpty(), "Const pattern should not be empty")
        }
    }

    @Test
    fun `unitPat generates unit pattern`() {
        val pat = SmlPatterns.unitPat(ctx())
        assertEquals("()", pat, "Unit pattern should be '()'")
    }

    @Test
    fun `nilPat generates nil pattern`() {
        val pat = SmlPatterns.nilPat(ctx())
        assertEquals("[]", pat, "Nil pattern should be '[]'")
    }

    @Test
    fun `atomicPattern generates valid patterns`() {
        repeat(50) { i ->
            val pat = SmlPatterns.atomicPattern(ctx(i.toLong()))
            assertTrue(pat.isNotEmpty(), "Atomic pattern should not be empty")
        }
    }

    @Test
    fun `pattern generates valid patterns`() {
        repeat(50) { i ->
            val pat = SmlPatterns.pattern(ctx(i.toLong()))
            assertTrue(pat.isNotEmpty(), "Pattern should not be empty")
        }
    }

    @Test
    fun `funArgPats generates function argument patterns`() {
        repeat(50) { i ->
            val pats = SmlPatterns.funArgPats(ctx(i.toLong()))
            assertTrue(pats.isNotEmpty(), "Fun arg patterns should not be empty")
        }
    }

    @Test
    fun `optTypeAnnotation generates optional type annotations`() {
        repeat(50) { i ->
            val ann = SmlPatterns.optTypeAnnotation(ctx(i.toLong()))
            // Can be empty or contain a type annotation
            assertTrue(ann.isEmpty() || ann.contains(":"), "Type annotation should be empty or contain ':'")
        }
    }

    @Test
    fun `pattern produces variety`() {
        val pats = (0L until 100L).map { SmlPatterns.pattern(ctx(it)) }.toSet()
        assertTrue(pats.size > 20, "Should produce varied patterns")
    }

    @Test
    fun `atomicPattern produces variety`() {
        val pats = (0L until 100L).map { SmlPatterns.atomicPattern(ctx(it)) }.toSet()
        assertTrue(pats.size > 20, "Should produce varied atomic patterns")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlPatterns.pattern(ctx(12345L))
        val result2 = SmlPatterns.pattern(ctx(12345L))
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
