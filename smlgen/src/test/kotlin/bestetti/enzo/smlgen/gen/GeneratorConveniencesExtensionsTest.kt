package bestetti.enzo.smlgen.gen

import bestetti.enzo.smlgen.gen.GeneratorConveniences.F
import bestetti.enzo.smlgen.gen.GeneratorConveniences.X
import bestetti.enzo.smlgen.gen.GeneratorConveniences.toGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for GeneratorConveniences extension functions.
 */
class GeneratorConveniencesExtensionsTest {

    private fun ctx(seed: Long = 42L) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = 5, maxRepeat = 3)
    )

    @Test
    fun `toGenerator converts string to literal generator`() {
        val gen = "hello".toGenerator()
        val result = gen(ctx())
        assertEquals("hello", result)
    }

    @Test
    fun `toGenerator works with empty string`() {
        val gen = "".toGenerator()
        val result = gen(ctx())
        assertEquals("", result)
    }

    @Test
    fun `String X Generator produces choice between string and generator`() {
        val gen = "left" X AtomicGenerators.literal("right")
        val results = (0L until 50L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.contains("left"), "Should produce 'left'")
        assertTrue(results.contains("right"), "Should produce 'right'")
    }

    @Test
    fun `Generator X String produces choice between generator and string`() {
        val gen = AtomicGenerators.literal("left") X "right"
        val results = (0L until 50L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.contains("left"), "Should produce 'left'")
        assertTrue(results.contains("right"), "Should produce 'right'")
    }

    @Test
    fun `String X String produces choice between two strings`() {
        val gen = "left" X "right"
        val results = (0L until 50L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.contains("left"), "Should produce 'left'")
        assertTrue(results.contains("right"), "Should produce 'right'")
    }

    @Test
    fun `String F Generator produces sequence of string and generator`() {
        val gen = "hello" F AtomicGenerators.literal(" world")
        val result = gen(ctx())
        assertEquals("hello world", result)
    }

    @Test
    fun `Generator F String produces sequence of generator and string`() {
        val gen = AtomicGenerators.literal("hello") F " world"
        val result = gen(ctx())
        assertEquals("hello world", result)
    }

    @Test
    fun `String F String produces sequence of two strings`() {
        val gen = "hello" F " world"
        val result = gen(ctx())
        assertEquals("hello world", result)
    }

    @Test
    fun `chained F operations work correctly`() {
        val gen = "a" F "b" F "c"
        val result = gen(ctx())
        assertEquals("abc", result)
    }

    @Test
    fun `chained X operations produce varied results`() {
        val gen = "a" X "b" X "c"
        val results = (0L until 100L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.size >= 2, "Should produce at least 2 different results")
    }

    @Test
    fun `mixed F and X operations work`() {
        val gen = ("prefix" F "_") X "other"
        val results = (0L until 50L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.contains("prefix_") || results.contains("other"))
    }

    @Test
    fun `String X Generator with complex generator`() {
        val complexGen = GeneratorCombinators.seq(
            AtomicGenerators.literal("a"),
            AtomicGenerators.literal("b")
        )
        val gen = "simple" X complexGen
        val results = (0L until 50L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.contains("simple") || results.contains("ab"))
    }

    @Test
    fun `Generator X String with complex generator`() {
        val complexGen = GeneratorCombinators.seq(
            AtomicGenerators.literal("a"),
            AtomicGenerators.literal("b")
        )
        val gen = complexGen X "simple"
        val results = (0L until 50L).map { gen(ctx(it)) }.toSet()
        assertTrue(results.contains("simple") || results.contains("ab"))
    }
}
