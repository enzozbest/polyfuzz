package bestetti.enzo.smlgen.gen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GenerationContextTest {

    @Test
    fun `Test GenerationConfig with default values`() {
        val config = GenerationConfig()
        assertEquals(10, config.maxDepth)
        assertEquals(5, config.maxRepeat)
    }

    @Test
    fun `Test GenerationConfig with custom values`() {
        val config = GenerationConfig(maxDepth = 20, maxRepeat = 10)
        assertEquals(20, config.maxDepth)
        assertEquals(10, config.maxRepeat)
    }

    @Test
    fun `Test GenerationContext with default values`() {
        val ctx = GenerationContext()
        assertEquals(10, ctx.config.maxDepth)
        assertEquals(5, ctx.config.maxRepeat)
        assertFalse(ctx.isAtMaxDepth)
    }

    @Test
    fun `Test GenerationContext with custom config`() {
        val config = GenerationConfig(maxDepth = 3, maxRepeat = 2)
        val ctx = GenerationContext(config = config)
        assertEquals(3, ctx.config.maxDepth)
        assertEquals(2, ctx.config.maxRepeat)
    }

    @Test
    fun `Test isAtMaxDepth returns false when depth is 0`() {
        val ctx = GenerationContext(config = GenerationConfig(maxDepth = 5))
        assertFalse(ctx.isAtMaxDepth)
    }

    @Test
    fun `Test isAtMaxDepth returns true when depth equals maxDepth`() {
        val config = GenerationConfig(maxDepth = 2)
        val ctx = GenerationContext(config = config)
        ctx.deeper {
            ctx.deeper {
                assertTrue(ctx.isAtMaxDepth)
            }
        }
    }

    @Test
    fun `Test isAtMaxDepth returns true when depth exceeds maxDepth`() {
        val config = GenerationConfig(maxDepth = 1)
        val ctx = GenerationContext(config = config)
        ctx.deeper {
            ctx.deeper {
                assertTrue(ctx.isAtMaxDepth)
            }
        }
    }

    @Test
    fun `Test recursiveProbability returns 0 when maxDepth is 0`() {
        val config = GenerationConfig(maxDepth = 0)
        val ctx = GenerationContext(config = config)
        assertEquals(0.0, ctx.recursiveProbability())
    }

    @Test
    fun `Test recursiveProbability returns 0 when maxDepth is negative`() {
        val config = GenerationConfig(maxDepth = -1)
        val ctx = GenerationContext(config = config)
        assertEquals(0.0, ctx.recursiveProbability())
    }

    @Test
    fun `Test recursiveProbability returns 1 at depth 0 with positive maxDepth`() {
        val config = GenerationConfig(maxDepth = 10)
        val ctx = GenerationContext(config = config)
        assertEquals(1.0, ctx.recursiveProbability())
    }

    @Test
    fun `Test recursiveProbability decreases as depth increases`() {
        val config = GenerationConfig(maxDepth = 10)
        val ctx = GenerationContext(config = config)
        
        val probAtDepth0 = ctx.recursiveProbability()
        
        ctx.deeper {
            val probAtDepth1 = ctx.recursiveProbability()
            assertTrue(probAtDepth1 < probAtDepth0)
            
            ctx.deeper {
                val probAtDepth2 = ctx.recursiveProbability()
                assertTrue(probAtDepth2 < probAtDepth1)
            }
        }
    }

    @Test
    fun `Test recursiveProbability approaches 0 at maxDepth`() {
        val config = GenerationConfig(maxDepth = 5)
        val ctx = GenerationContext(config = config)

        ctx.deeper {
            assertNotEquals(0.0, ctx.recursiveProbability())
            ctx.deeper {
                assertNotEquals(0.0, ctx.recursiveProbability())
                ctx.deeper {
                    assertNotEquals(0.0, ctx.recursiveProbability())
                    ctx.deeper {
                        assertNotEquals(0.0, ctx.recursiveProbability())
                        ctx.deeper {
                            assertEquals(0.0, ctx.recursiveProbability())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Test deeper increments and decrements depth correctly`() {
        val config = GenerationConfig(maxDepth = 10)
        val ctx = GenerationContext(config = config)

        val prob0 = ctx.recursiveProbability()
        assertEquals(1.0, prob0)
        
        ctx.deeper {
            val prob1 = ctx.recursiveProbability()
            assertTrue(prob1 < prob0)
        }

        assertEquals(1.0, ctx.recursiveProbability())
    }

    @Test
    fun `Test deeper returns the value from the block`() {
        val ctx = GenerationContext()
        val result = ctx.deeper { "test result" }
        assertEquals("test result", result)
    }

    @Test
    fun `Test deeper restores depth even when exception is thrown`() {
        val ctx = GenerationContext()
        val initialProb = ctx.recursiveProbability()
        
        try {
            ctx.deeper {
                throw RuntimeException("test exception")
            }
        } catch (_: RuntimeException) {
            // Expected
        }
        assertEquals(initialProb, ctx.recursiveProbability())
    }

    @Test
    fun `Test nested deeper calls work correctly`() {
        val config = GenerationConfig(maxDepth = 10)
        val ctx = GenerationContext(config = config)
        
        val depths = mutableListOf<Double>()
        depths.add(ctx.recursiveProbability())
        
        ctx.deeper {
            depths.add(ctx.recursiveProbability())
            ctx.deeper {
                depths.add(ctx.recursiveProbability())
                ctx.deeper {
                    depths.add(ctx.recursiveProbability())
                }
                depths.add(ctx.recursiveProbability())
            }
            depths.add(ctx.recursiveProbability())
        }
        depths.add(ctx.recursiveProbability())
        
        assertTrue(depths[0] > depths[1])
        assertTrue(depths[1] > depths[2])
        assertTrue(depths[2] > depths[3])
        assertEquals(depths[2], depths[4])
        assertEquals(depths[1], depths[5])
        assertEquals(depths[0], depths[6])
    }
}
