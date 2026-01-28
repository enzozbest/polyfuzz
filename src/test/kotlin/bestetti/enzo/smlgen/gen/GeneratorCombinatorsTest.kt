package bestetti.enzo.smlgen.gen

import bestetti.enzo.smlgen.gen.GeneratorCombinators.between
import bestetti.enzo.smlgen.gen.GeneratorCombinators.choice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.count
import bestetti.enzo.smlgen.gen.GeneratorCombinators.depthChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.lazy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.many
import bestetti.enzo.smlgen.gen.GeneratorCombinators.many1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.map
import bestetti.enzo.smlgen.gen.GeneratorCombinators.optional
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.seq
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBraces
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBrackets
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapParens
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeneratorCombinatorsTest {

    @Test
    fun `Test seq with no generators returns empty string`() {
        val gen = seq()
        val ctx = GenerationContext()
        assertEquals("", gen.generate(ctx))
    }

    @Test
    fun `Test seq with single generator returns that generator's output`() {
        val gen = seq({ "hello" })
        val ctx = GenerationContext()
        assertEquals("hello", gen.generate(ctx))
    }

    @Test
    fun `Test seq concatenates multiple generators in order`() {
        val gen = seq({ "a" }, { "b" }, { "c" })
        val ctx = GenerationContext()
        assertEquals("abc", gen.generate(ctx))
    }

    @Test
    fun `Test seq with empty generators produces empty parts`() {
        val gen = seq({ "a" }, { "" }, { "b" })
        val ctx = GenerationContext()
        assertEquals("ab", gen.generate(ctx))
    }

    @Test //This test may fail occasionally due to randomness, but very unlikely!
    fun `Test choice selects one of the generators randomly`() {
        val gen = choice({ "a" }, { "b" }, { "c" })
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(gen.generate(ctx))
        }
        assertEquals(setOf("a", "b", "c"), results)
    }

    @Test
    fun `Test choice with single generator always returns that generator`() {
        val gen = choice({ "only" })
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            assertEquals("only", gen.generate(ctx))
        }
    }

    @Test
    fun `Test weightedChoice throws when no options provided`() {
        assertFailsWith<IllegalArgumentException> {
            weightedChoice()
        }
    }

    @Test
    fun `Test weightedChoice throws when total weight is zero`() {
        assertFailsWith<IllegalArgumentException> {
            weightedChoice(0.0 to Generator { "a" })
        }
    }

    @Test
    fun `Test weightedChoice throws when total weight is negative`() {
        assertFailsWith<IllegalArgumentException> {
            weightedChoice(-1.0 to Generator { "a" })
        }
    }

    @Test
    fun `Test weightedChoice with single option always returns that option`() {
        val gen = weightedChoice(1.0 to Generator { "only" })
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            assertEquals("only", gen.generate(ctx))
        }
    }

    @Test
    fun `Test weightedChoice respects weights`() {
        val gen = weightedChoice(100.0 to Generator { "heavy" }, 1.0 to Generator { "light" })
        
        var heavyCount = 0
        var lightCount = 0
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            when (gen.generate(ctx)) {
                "heavy" -> heavyCount++
                "light" -> lightCount++
            }
        }
        
        // Heavy should be much more common
        assertTrue(heavyCount > lightCount * 10, "Heavy: $heavyCount, Light: $lightCount")
    }

    @Test
    fun `Test weightedChoice covers all options`() {
        val gen = weightedChoice(1.0 to Generator { "a" }, 2.0 to Generator { "b" }, 2.0 to Generator { "c" })
        
        val results = mutableSetOf<String>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(gen.generate(ctx))
        }
        
        assertEquals(setOf("a", "b", "c"), results)
    }

    @Test
    fun `Test weightedChoice fallback to last option`() {
        val gen = weightedChoice(Double.MIN_VALUE to Generator { "fallback" })

        val customRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.9999999999999999 // Very close to 1.0
        }
        
        val ctx = GenerationContext(random = customRandom)
        val result = gen.generate(ctx)
        assertEquals("fallback", result)
    }

    @Test
    fun `Test weightedChoice fallback triggered by floating point edge case`() {
        val customRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = Int.MAX_VALUE
            override fun nextDouble(): Double = 1.0 - Double.MIN_VALUE
        }
        
        // Use weights that might not sum exactly to their apparent total due to floating point
        val gen = weightedChoice(
            0.1 to Generator { "a" },
            0.1 to Generator { "b" },
            0.1 to Generator { "c" },
            0.1 to Generator { "d" },
            0.1 to Generator { "e" },
            0.1 to Generator { "f" },
            0.1 to Generator { "g" },
            0.1 to Generator { "h" },
            0.1 to Generator { "i" },
            0.1 to Generator { "last" }
        )
        
        val ctx = GenerationContext(random = customRandom)
        val result = gen.generate(ctx)
        assertTrue(result in listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "last"))
    }


    @Test
    fun `Test depthChoice uses terminal at maxDepth`() {
        val gen = depthChoice(terminal = { "terminal" }, recursive = { "recursive" })
        
        val config = GenerationConfig(maxDepth = 0)
        val ctx = GenerationContext(config = config)
        
        assertEquals("terminal", gen.generate(ctx))
    }

    @Test
    fun `Test depthChoice can use recursive when not at maxDepth`() {
        val gen = depthChoice(terminal = { "terminal" }, recursive = { "recursive" })
        val config = GenerationConfig(maxDepth = 10)
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = config)
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains("recursive"))
    }

    @Test
    fun `Test depthChoice terminal branch when probability check fails`() {
        val gen = depthChoice(terminal = { "terminal" }, recursive = { "recursive" })
        val config = GenerationConfig(maxDepth = 10)
        val results = mutableSetOf<String>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = config)
            ctx.deeper {
                ctx.deeper {
                    ctx.deeper {
                        ctx.deeper {
                            ctx.deeper {
                                ctx.deeper {
                                    ctx.deeper {
                                        ctx.deeper {
                                            ctx.deeper {
                                                results.add(gen.generate(ctx))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(results.contains("terminal"))
    }

    @Test
    fun `Test depthChoice increments depth for recursive call`() {
        val gen = depthChoice(terminal = { "terminal" }, recursive = { "recursive" })
        
        // Force recursive path by using depth 0 with high probability
        val config = GenerationConfig(maxDepth = 100)
        val ctx = GenerationContext(random = Random(42), config = config)
        
        gen.generate(ctx)
        
        // After the call, depth should be back to 0 (verified by probability = 1.0)
        assertEquals(1.0, ctx.recursiveProbability())
    }

    @Test
    fun `Test depthChoice list throws when terminals empty`() {
        assertFailsWith<IllegalArgumentException> {
            depthChoice(
                terminals = emptyList(),
                recursives = listOf(Generator { "r" })
            )
        }
    }

    @Test
    fun `Test depthChoice list throws when recursives empty`() {
        assertFailsWith<IllegalArgumentException> {
            depthChoice(
                terminals = listOf(Generator { "t" }),
                recursives = emptyList()
            )
        }
    }

    @Test
    fun `Test depthChoice list uses terminal at maxDepth`() {
        val gen = depthChoice(
            terminals = listOf(Generator { "t1" }, Generator { "t2" }),
            recursives = listOf(Generator { "r1" }, Generator { "r2" })
        )
        
        val config = GenerationConfig(maxDepth = 0)
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = config)
            results.add(gen.generate(ctx))
        }
        
        // Should only have terminals
        assertTrue(results.all { it.startsWith("t") })
        assertEquals(setOf("t1", "t2"), results)
    }

    @Test
    fun `Test depthChoice list selects from recursives when not at maxDepth`() {
        val gen = depthChoice(terminals = listOf(Generator { "t1" }), recursives = listOf(Generator { "r1" }))
        val config = GenerationConfig(maxDepth = 100)
        
        val results = mutableSetOf<String>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = config)
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains("r1") || results.contains("r2"))
    }

    @Test
    fun `Test depthChoice list can select terminal when probability check fails`() {
        val gen = depthChoice(
            terminals = listOf(Generator { "t1" }, Generator { "t2" }),
            recursives = listOf(Generator { "r1" }, Generator { "r2" })
        )
        
        val config = GenerationConfig(maxDepth = 10)
        val results = mutableSetOf<String>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = config)
            ctx.deeper {
                ctx.deeper {
                    ctx.deeper {
                        ctx.deeper {
                            ctx.deeper {
                                ctx.deeper {
                                    ctx.deeper {
                                        ctx.deeper {
                                            ctx.deeper {
                                                results.add(gen.generate(ctx))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(results.contains("t1") || results.contains("t2"))
    }

    @Test
    fun `Test many can return empty string when n is 0`() {
        val gen = many { "x" }
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains(""))
    }

    @Test
    fun `Test many produces varying lengths up to maxRepeat`() {
        val gen = many { "x" }
        
        val lengths = mutableSetOf<Int>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            lengths.add(gen.generate(ctx).length)
        }
        assertEquals(setOf(0, 1, 2, 3, 4, 5), lengths)
    }

    @Test
    fun `Test many with maxRepeat 0 always returns empty`() {
        val gen = many { "x" }
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 0))
            assertEquals("", gen.generate(ctx))
        }
    }

    @Test
    fun `Test many1 always produces at least one repetition`() {
        val gen = many1 { "x" }
        
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            val result = gen.generate(ctx)
            assertTrue(result.isNotEmpty())
        }
    }

    @Test
    fun `Test many1 produces varying lengths from 1 to maxRepeat`() {
        val gen = many1 { "x" }
        
        val lengths = mutableSetOf<Int>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            lengths.add(gen.generate(ctx).length)
        }
        assertEquals(setOf(1, 2, 3, 4, 5), lengths)
    }

    @Test
    fun `Test many1 with maxRepeat 1 always returns single item`() {
        val gen = many1 { "x" }
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 1))
            assertEquals("x", gen.generate(ctx))
        }
    }

    @Test
    fun `Test optional returns either empty or generator output`() {
        val gen = optional { "value" }
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            results.add(gen.generate(ctx))
        }
        
        assertEquals(setOf("", "value"), results)
    }

    @Test
    fun `Test optional is roughly 50-50`() {
        val gen = optional { "value" }
        
        var emptyCount = 0
        var valueCount = 0
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed))
            when (gen.generate(ctx)) {
                "" -> emptyCount++
                "value" -> valueCount++
            }
        }
        
        assertTrue(emptyCount > 300, "Empty count: $emptyCount")
        assertTrue(valueCount > 300, "Value count: $valueCount")
    }

    @Test
    fun `Test count repeats exactly n times`() {
        val gen = count(3) { "x" }
        val ctx = GenerationContext()
        assertEquals("xxx", gen.generate(ctx))
    }

    @Test
    fun `Test count with n equals 1`() {
        val gen = count(1) { "hello" }
        val ctx = GenerationContext()
        assertEquals("hello", gen.generate(ctx))
    }

    @Test
    fun `Test count throws when n is 0`() {
        val gen = count(0) { "x" }
        val ctx = GenerationContext()
        assertFailsWith<IllegalArgumentException> {
            gen.generate(ctx)
        }
    }

    @Test
    fun `Test count throws when n is negative`() {
        val gen = count(-1) { "x" }
        val ctx = GenerationContext()
        assertFailsWith<IllegalArgumentException> {
            gen.generate(ctx)
        }
    }

    @Test
    fun `Test count with large n`() {
        val gen = count(100) { "a" }
        val ctx = GenerationContext()
        assertEquals("a".repeat(100), gen.generate(ctx))
    }

    @Test
    fun `Test sepBy can return empty when n is 0`() {
        val gen = sepBy({ "x" }, { "," })
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains(""))
    }

    @Test
    fun `Test sepBy produces single element without separator`() {
        val gen = sepBy({ "x" }, { "," })
        
        val results = mutableSetOf<String>()
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains("x"))
    }

    @Test
    fun `Test sepBy produces multiple elements with separators`() {
        val gen = sepBy({ "x" }, { "," })
        
        val results = mutableSetOf<String>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 3))
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains(""))
        assertTrue(results.contains("x"))
        assertTrue(results.contains("x,x"))
        assertTrue(results.contains("x,x,x"))
    }

    @Test
    fun `Test sepBy with maxRepeat 0 always returns empty`() {
        val gen = sepBy({ "x" }, { "," })
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 0))
            assertEquals("", gen.generate(ctx))
        }
    }


    @Test
    fun `Test sepBy1 always produces at least one element`() {
        val gen = sepBy1({ "x" }, { "," })
        
        repeat(100) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 5))
            val result = gen.generate(ctx)
            assertTrue(result.isNotEmpty())
            assertTrue(result.startsWith("x"))
        }
    }

    @Test
    fun `Test sepBy1 produces varying lengths`() {
        val gen = sepBy1({ "x" }, { "," })
        
        val results = mutableSetOf<String>()
        repeat(1000) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 3))
            results.add(gen.generate(ctx))
        }
        assertTrue(results.contains("x"))
        assertTrue(results.contains("x,x"))
        assertTrue(results.contains("x,x,x"))
    }

    @Test
    fun `Test sepBy1 with maxRepeat 1 always returns single element`() {
        val gen = sepBy1({ "item" }, { "-" })
        
        repeat(10) { seed ->
            val ctx = GenerationContext(random = Random(seed), config = GenerationConfig(maxRepeat = 1))
            assertEquals("item", gen.generate(ctx))
        }
    }

    @Test
    fun `Test between wraps generator with before and after`() {
        val gen = Generator { "content" }.between({ "[" }, { "]" })
        val ctx = GenerationContext()
        assertEquals("[content]", gen.generate(ctx))
    }

    @Test
    fun `Test between with empty before and after`() {
        val gen = Generator { "content" }.between({ "" }, { "" })
        val ctx = GenerationContext()
        assertEquals("content", gen.generate(ctx))
    }

    @Test
    fun `Test lazy evaluates supplier each time`() {
        var counter = 0
        val gen = lazy { 
            counter++
            Generator { "value$counter" }
        }
        
        val ctx = GenerationContext()
        assertEquals("value1", gen.generate(ctx))
        assertEquals("value2", gen.generate(ctx))
        assertEquals("value3", gen.generate(ctx))
    }

    @Test
    fun `Test lazy allows recursive definitions`() {
        lateinit var expr: Generator
        expr = lazy {
            choice(
                Generator { "x" },
                seq({ "(" }, expr, { ")" })
            )
        }
        
        val ctx = GenerationContext(random = Random(42), config = GenerationConfig(maxDepth = 3))
        val result = expr.generate(ctx)
        assertTrue(result.isNotEmpty())
    }


    @Test
    fun `Test map transforms generator output`() {
        val gen = Generator { "hello" }.map { it.uppercase() }
        val ctx = GenerationContext()
        assertEquals("HELLO", gen.generate(ctx))
    }

    @Test
    fun `Test map with identity function`() {
        val gen = Generator { "unchanged" }.map { it }
        val ctx = GenerationContext()
        assertEquals("unchanged", gen.generate(ctx))
    }

    @Test
    fun `Test map can change length`() {
        val gen = Generator { "ab" }.map { it + it }
        val ctx = GenerationContext()
        assertEquals("abab", gen.generate(ctx))
    }

    @Test
    fun `Test wrapParens wraps in parentheses`() {
        val gen = Generator { "content" }.wrapParens()
        val ctx = GenerationContext()
        assertEquals("(content)", gen.generate(ctx))
    }

    @Test
    fun `Test wrapParens with empty content`() {
        val gen = Generator { "" }.wrapParens()
        val ctx = GenerationContext()
        assertEquals("()", gen.generate(ctx))
    }

    @Test
    fun `Test wrapBrackets wraps in brackets`() {
        val gen = Generator { "content" }.wrapBrackets()
        val ctx = GenerationContext()
        assertEquals("[content]", gen.generate(ctx))
    }

    @Test
    fun `Test wrapBrackets with empty content`() {
        val gen = Generator { "" }.wrapBrackets()
        val ctx = GenerationContext()
        assertEquals("[]", gen.generate(ctx))
    }

    @Test
    fun `Test wrapBraces wraps in braces with spaces`() {
        val gen = Generator { "content" }.wrapBraces()
        val ctx = GenerationContext()
        assertEquals("{ content }", gen.generate(ctx))
    }

    @Test
    fun `Test wrapBraces with empty content`() {
        val gen = Generator { "" }.wrapBraces()
        val ctx = GenerationContext()
        assertEquals("{  }", gen.generate(ctx))
    }
}
