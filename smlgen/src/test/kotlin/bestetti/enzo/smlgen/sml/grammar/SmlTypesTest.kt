package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for SmlTypes generators.
 */
class SmlTypesTest {

    private fun ctx(seed: Long = 42L, maxDepth: Int = 5, maxRepeat: Int = 3) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = maxDepth, maxRepeat = maxRepeat)
    )

    @Test
    fun `tyVar generates type variables`() {
        repeat(50) { i ->
            val tv = SmlTypes.tyVar(ctx(i.toLong()))
            assertTrue(tv.startsWith("'"), "Type var should start with ': $tv")
        }
    }

    @Test
    fun `atomicType generates valid types`() {
        repeat(50) { i ->
            val ty = SmlTypes.atomicType(ctx(i.toLong()))
            assertTrue(ty.isNotEmpty(), "Atomic type should not be empty")
        }
    }

    @Test
    fun `type generates valid types`() {
        repeat(50) { i ->
            val ty = SmlTypes.type(ctx(i.toLong()))
            assertTrue(ty.isNotEmpty(), "Type should not be empty")
        }
    }

    @Test
    fun `tyVarSeq generates type variable sequences`() {
        repeat(50) { i ->
            val seq = SmlTypes.tyVarSeq(ctx(i.toLong()))
            // Can be empty or contain type variables
            assertTrue(
                seq.isEmpty() || seq.contains("'") || seq.startsWith("("),
                "Type var seq should be empty or contain type variables: $seq"
            )
        }
    }

    @Test
    fun `listType generates list types`() {
        repeat(50) { i ->
            val ty = SmlTypes.listType(ctx(i.toLong()))
            assertTrue(ty.contains("list"), "List type should contain 'list': $ty")
        }
    }

    @Test
    fun `optionType generates option types`() {
        repeat(50) { i ->
            val ty = SmlTypes.optionType(ctx(i.toLong()))
            assertTrue(ty.contains("option"), "Option type should contain 'option': $ty")
        }
    }

    @Test
    fun `refType generates ref types`() {
        repeat(50) { i ->
            val ty = SmlTypes.refType(ctx(i.toLong()))
            assertTrue(ty.contains("ref"), "Ref type should contain 'ref': $ty")
        }
    }

    @Test
    fun `commonType generates common types`() {
        repeat(50) { i ->
            val ty = SmlTypes.commonType(ctx(i.toLong()))
            assertTrue(ty.isNotEmpty(), "Common type should not be empty")
        }
    }

    @Test
    fun `type produces variety`() {
        val types = (0L until 100L).map { SmlTypes.type(ctx(it)) }.toSet()
        assertTrue(types.size > 20, "Should produce varied types")
    }

    @Test
    fun `atomicType produces variety`() {
        val types = (0L until 100L).map { SmlTypes.atomicType(ctx(it)) }.toSet()
        assertTrue(types.size > 20, "Should produce varied atomic types")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlTypes.type(ctx(12345L))
        val result2 = SmlTypes.type(ctx(12345L))
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
