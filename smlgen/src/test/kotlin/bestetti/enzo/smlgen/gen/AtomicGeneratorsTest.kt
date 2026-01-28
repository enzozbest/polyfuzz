package bestetti.enzo.smlgen.gen

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtomicGeneratorsTest {

    @Test
    fun `Test literal returns the given string`() {
        val gen = AtomicGenerators.literal("hello")
        val ctx = GenerationContext()
        assertEquals("hello", gen.generate(ctx))
    }

    @Test
    fun `Test literal with empty string`() {
        val gen = AtomicGenerators.literal("")
        val ctx = GenerationContext()
        assertEquals("", gen.generate(ctx))
    }

    @Test
    fun `Test literal with special characters`() {
        val gen = AtomicGenerators.literal("hello\nworld\ttab")
        val ctx = GenerationContext()
        assertEquals("hello\nworld\ttab", gen.generate(ctx))
    }

    @Test
    fun `Test literal with unicode`() {
        val gen = AtomicGenerators.literal("こんにちは")
        val ctx = GenerationContext()
        assertEquals("こんにちは", gen.generate(ctx))
    }

    @Test
    fun `Test empty returns empty string`() {
        val gen = AtomicGenerators.empty()
        val ctx = GenerationContext()
        assertEquals("", gen.generate(ctx))
    }

    @Test
    fun `Test empty always returns empty string regardless of context`() {
        val gen = AtomicGenerators.empty()
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            assertEquals("", gen.generate(ctx))
        }
    }

    @Test
    fun `Test ws returns single space`() {
        val gen = AtomicGenerators.ws()
        val ctx = GenerationContext()
        assertEquals(" ", gen.generate(ctx))
    }

    @Test
    fun `Test ws always returns single space regardless of context`() {
        val gen = AtomicGenerators.ws()
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            assertEquals(" ", gen.generate(ctx))
        }
    }

    @Test
    fun `Test oneOf selects from provided strings correctly`() {
        val gen = AtomicGenerators.oneOf("a", "b", "c")
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(gen.generate(ctx))
        }

        val allowed = setOf("a", "b", "c")
        val onlyAllowedCharacters = allowed.union(results) == allowed
        assertTrue(onlyAllowedCharacters && results.isNotEmpty())
    }

    @Test
    fun `Test oneOf with single string always returns that string`() {
        val gen = AtomicGenerators.oneOf("only")
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            assertEquals("only", gen.generate(ctx))
        }
    }

    @Test
    fun `Test oneOf is deterministic with same random seed`() {
        val gen = AtomicGenerators.oneOf("a", "b", "c", "d", "e")
        
        val ctx1 = GenerationContext(random = Random(42))
        val ctx2 = GenerationContext(random = Random(42))
        
        assertEquals(gen.generate(ctx1), gen.generate(ctx2))
    }
}
