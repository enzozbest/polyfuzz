package bestetti.enzo.smlgen.sml.generator

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for SmlProgramGeneratorConveniences.
 */
class SmlProgramGeneratorConveniencesTest {

    @Test
    fun `generateMinimal produces short programs`() {
        repeat(20) { i ->
            val program = SmlProgramGeneratorConveniences.generateMinimal(maxLength = 100, seed = i.toLong())
            assertTrue(program.length <= 100, "Minimal program should respect max length: ${program.length}")
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `generateMinimal with default parameters works`() {
        val program = SmlProgramGeneratorConveniences.generateMinimal()
        assertTrue(program.isNotEmpty(), "Program should not be empty")
    }

    @Test
    fun `generateSimple produces programs`() {
        repeat(20) { i ->
            val program = SmlProgramGeneratorConveniences.generateSimple(maxLength = 200, seed = i.toLong())
            assertTrue(program.length <= 200, "Simple program should respect max length: ${program.length}")
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `generateSimple with default parameters works`() {
        val program = SmlProgramGeneratorConveniences.generateSimple()
        assertTrue(program.isNotEmpty(), "Program should not be empty")
    }

    @Test
    fun `generateMedium produces programs`() {
        repeat(20) { i ->
            val program = SmlProgramGeneratorConveniences.generateMedium(maxLength = 500, seed = i.toLong())
            assertTrue(program.length <= 500, "Medium program should respect max length: ${program.length}")
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `generateMedium with default parameters works`() {
        val program = SmlProgramGeneratorConveniences.generateMedium()
        assertTrue(program.isNotEmpty(), "Program should not be empty")
    }

    @Test
    fun `generateComplex produces programs`() {
        repeat(10) { i ->
            val program = SmlProgramGeneratorConveniences.generateComplex(maxLength = 1000, seed = i.toLong())
            assertTrue(program.length <= 1000, "Complex program should respect max length: ${program.length}")
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `generateComplex with default parameters works`() {
        val program = SmlProgramGeneratorConveniences.generateComplex()
        assertTrue(program.isNotEmpty(), "Program should not be empty")
    }

    @Test
    fun `generateExtreme produces programs`() {
        repeat(10) { i ->
            val program = SmlProgramGeneratorConveniences.generateExtreme(maxLength = 2000, seed = i.toLong())
            assertTrue(program.length <= 2000, "Extreme program should respect max length: ${program.length}")
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `generateExtreme with default parameters works`() {
        val program = SmlProgramGeneratorConveniences.generateExtreme()
        assertTrue(program.isNotEmpty(), "Program should not be empty")
    }

    @Test
    fun `generateMinimal produces variety with different seeds`() {
        val programs = (0L until 20L).map { SmlProgramGeneratorConveniences.generateMinimal(seed = it) }.toSet()
        assertTrue(programs.size > 5, "Should produce varied programs")
    }

    @Test
    fun `generateSimple produces variety with different seeds`() {
        val programs = (0L until 20L).map { SmlProgramGeneratorConveniences.generateSimple(seed = it) }.toSet()
        assertTrue(programs.size > 5, "Should produce varied programs")
    }

    @Test
    fun `generateMedium produces variety with different seeds`() {
        val programs = (0L until 20L).map { SmlProgramGeneratorConveniences.generateMedium(seed = it) }.toSet()
        assertTrue(programs.size > 5, "Should produce varied programs")
    }

    @Test
    fun `generateComplex produces variety with different seeds`() {
        val programs = (0L until 10L).map { SmlProgramGeneratorConveniences.generateComplex(seed = it) }.toSet()
        assertTrue(programs.size > 3, "Should produce varied programs")
    }

    @Test
    fun `generateExtreme produces variety with different seeds`() {
        val programs = (0L until 10L).map { SmlProgramGeneratorConveniences.generateExtreme(seed = it) }.toSet()
        assertTrue(programs.size > 3, "Should produce varied programs")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val seed = 12345L
        val result1 = SmlProgramGeneratorConveniences.generateMinimal(seed = seed)
        val result2 = SmlProgramGeneratorConveniences.generateMinimal(seed = seed)
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
