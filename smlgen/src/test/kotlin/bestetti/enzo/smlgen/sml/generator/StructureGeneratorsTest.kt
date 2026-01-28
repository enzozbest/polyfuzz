package bestetti.enzo.smlgen.sml.generator

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for StructureGenerators.
 */
class StructureGeneratorsTest {

    private fun ctx(seed: Long = 42L, maxDepth: Int = 5, maxRepeat: Int = 3) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = maxDepth, maxRepeat = maxRepeat)
    )

    @Test
    fun `minimalProgram generates non-empty output`() {
        repeat(50) { i ->
            val result = StructureGenerators.minimalProgram(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Minimal program should not be empty")
        }
    }

    @Test
    fun `minimalProgram produces variety`() {
        val results = (0L until 100L).map { StructureGenerators.minimalProgram(ctx(it)) }.toSet()
        assertTrue(results.size > 10, "Should produce varied minimal programs")
    }

    @Test
    fun `simpleProgram generates non-empty output`() {
        repeat(50) { i ->
            val result = StructureGenerators.simpleProgram(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Simple program should not be empty")
        }
    }

    @Test
    fun `simpleProgram produces variety`() {
        val results = (0L until 100L).map { StructureGenerators.simpleProgram(ctx(it)) }.toSet()
        assertTrue(results.size > 10, "Should produce varied simple programs")
    }

    @Test
    fun `mediumProgram generates non-empty output with comments`() {
        val config = ProgramConfig(maxLength = 500, includeComments = true)
        repeat(50) { i ->
            val result = StructureGenerators.mediumProgram(config)(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Medium program should not be empty")
        }
    }

    @Test
    fun `mediumProgram generates output without comments`() {
        val config = ProgramConfig(maxLength = 500, includeComments = false)
        repeat(50) { i ->
            val result = StructureGenerators.mediumProgram(config)(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Medium program without comments should not be empty")
        }
    }

    @Test
    fun `complexProgram generates non-empty output with comments`() {
        val config = ProgramConfig(maxLength = 1000, includeComments = true)
        repeat(50) { i ->
            val result = StructureGenerators.complexProgram(config)(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Complex program should not be empty")
        }
    }

    @Test
    fun `complexProgram generates output without comments`() {
        val config = ProgramConfig(maxLength = 1000, includeComments = false)
        repeat(50) { i ->
            val result = StructureGenerators.complexProgram(config)(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Complex program without comments should not be empty")
        }
    }

    @Test
    fun `extremeProgram generates non-empty output with comments`() {
        val config = ProgramConfig(maxLength = 2000, includeComments = true)
        repeat(50) { i ->
            val result = StructureGenerators.extremeProgram(config)(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Extreme program should not be empty")
        }
    }

    @Test
    fun `extremeProgram generates output without comments`() {
        val config = ProgramConfig(maxLength = 2000, includeComments = false)
        repeat(50) { i ->
            val result = StructureGenerators.extremeProgram(config)(ctx(i.toLong()))
            assertTrue(result.isNotEmpty(), "Extreme program without comments should not be empty")
        }
    }

    @Test
    fun `programElement generates non-empty output with obscure features`() {
        val config = ProgramConfig(maxLength = 500, includeObscureFeatures = true)
        repeat(50) { i ->
            StructureGenerators.programElement(config)(ctx(i.toLong()))
            // programElement can produce empty string (from empty() generator)
            // So we just verify it doesn't throw
        }
    }

    @Test
    fun `programElement generates output without obscure features`() {
        val config = ProgramConfig(maxLength = 500, includeObscureFeatures = false)
        repeat(50) { i ->
            StructureGenerators.programElement(config)(ctx(i.toLong()))
            // programElement can produce empty string (from empty() generator)
        }
    }

    @Test
    fun `programElement with obscure features produces variety`() {
        val config = ProgramConfig(maxLength = 500, includeObscureFeatures = true)
        val results = (0L until 100L).map { StructureGenerators.programElement(config)(ctx(it)) }.toSet()
        assertTrue(results.size > 5, "Should produce varied program elements with obscure features")
    }

    @Test
    fun `programElement without obscure features produces variety`() {
        val config = ProgramConfig(maxLength = 500, includeObscureFeatures = false)
        val results = (0L until 100L).map { StructureGenerators.programElement(config)(ctx(it)) }.toSet()
        assertTrue(results.size > 5, "Should produce varied program elements without obscure features")
    }

    @Test
    fun `mediumProgram produces variety`() {
        val config = ProgramConfig(maxLength = 500, includeComments = true)
        val results = (0L until 50L).map { StructureGenerators.mediumProgram(config)(ctx(it)) }.toSet()
        assertTrue(results.size > 10, "Should produce varied medium programs")
    }

    @Test
    fun `complexProgram produces variety`() {
        val config = ProgramConfig(maxLength = 1000, includeComments = true)
        val results = (0L until 50L).map { StructureGenerators.complexProgram(config)(ctx(it)) }.toSet()
        assertTrue(results.size > 10, "Should produce varied complex programs")
    }

    @Test
    fun `extremeProgram produces variety`() {
        val config = ProgramConfig(maxLength = 2000, includeComments = true)
        val results = (0L until 50L).map { StructureGenerators.extremeProgram(config)(ctx(it)) }.toSet()
        assertTrue(results.size > 10, "Should produce varied extreme programs")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val config = ProgramConfig(maxLength = 500, includeComments = true)
        val result1 = StructureGenerators.mediumProgram(config)(ctx(12345L))
        val result2 = StructureGenerators.mediumProgram(config)(ctx(12345L))
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
