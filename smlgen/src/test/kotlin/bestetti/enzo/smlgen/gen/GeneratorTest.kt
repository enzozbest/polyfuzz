package bestetti.enzo.smlgen.gen

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorTest {

    @Test
    fun `Test generate produces expected output`() {
        val gen = Generator { "hello" }
        val ctx = GenerationContext(random = Random(42))
        assertEquals("hello", gen.generate(ctx))
    }

    @Test
    fun `Test invoke operator calls generate`() {
        val gen = Generator { ctx -> "value-${ctx.config.maxDepth}" }
        val ctx = GenerationContext(config = GenerationConfig(maxDepth = 5))
        assertEquals("value-5", gen(ctx))
    }

    @Test
    fun `Test F infix operator sequences two generators`() {
        val gen1 = Generator { "hello" }
        val gen2 = Generator { " world" }
        val combined = gen1 F gen2
        val ctx = GenerationContext()
        assertEquals("hello world", combined.generate(ctx))
    }

    @Test
    fun `Test F infix operator chains multiple generators`() {
        val gen1 = Generator { "a" }
        val gen2 = Generator { "b" }
        val gen3 = Generator { "c" }
        val combined = gen1 F gen2 F gen3
        val ctx = GenerationContext()
        assertEquals("abc", combined.generate(ctx))
    }

    @Test
    fun `Test X infix operator creates choice between generators`() {
        val gen1 = Generator { "first" }
        val gen2 = Generator { "second" }
        val combined = gen1 X gen2
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(combined.generate(ctx))
        }
        assertEquals(results.union(setOf("first", "second")), setOf("first", "second"))
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `Test X infix operator chains multiple generators`() {
        val gen1 = Generator { "a" }
        val gen2 = Generator { "b" }
        val gen3 = Generator { "c" }
        val combined = gen1 X gen2 X gen3
        
        // Run multiple times with different seeds and collect results
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(combined.generate(ctx))
        }

        assertEquals(results.union(setOf("a", "b", "c")), setOf("a", "b", "c"))
        assertTrue(results.isNotEmpty())
    }

    @Test //This test might fail occasionally due to randomness, but very unlikely!
    fun `Test combined F and X operators work together`() {
        val hello = Generator { "hello" }
        val space = Generator { " " }
        val exclaim = Generator { "!" }

        val combined = hello F (space X exclaim)
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(combined.generate(ctx))
        }
        
        assertTrue(results.contains("hello "))
        assertTrue(results.contains("hello!"))
        assertEquals(results.union(setOf("hello ", "hello!")), setOf("hello ", "hello!"))
    }

    @Test
    fun `Test generator with context access`() {
        val gen = Generator { ctx ->
            if (ctx.isAtMaxDepth) "max" else "not-max"
        }
        
        val ctx1 = GenerationContext(config = GenerationConfig(maxDepth = 0))
        assertEquals("max", gen(ctx1))
        
        val ctx2 = GenerationContext(config = GenerationConfig(maxDepth = 10))
        assertEquals("not-max", gen(ctx2))
    }

    @Test //This test might fail occasionally due to randomness, but very unlikely!
    fun `Test generator using random from context`() {
        val gen = Generator { ctx ->
            ctx.random.nextInt(100).toString()
        }
        
        val ctx1 = GenerationContext(random = Random(42))
        val ctx2 = GenerationContext(random = Random(42))
        assertEquals(gen(ctx1), gen(ctx2))
        
        val results = mutableSetOf<String>()
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(gen(ctx))
        }
        assertTrue(results.size > 1)
    }
}
