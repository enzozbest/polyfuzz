package bestetti.enzo.smlgen.sml.generator

import bestetti.enzo.smlgen.gen.GenerationContext
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.sml.generator.StructureGenerators.complexProgram
import bestetti.enzo.smlgen.sml.generator.StructureGenerators.extremeProgram
import bestetti.enzo.smlgen.sml.generator.StructureGenerators.mediumProgram
import bestetti.enzo.smlgen.sml.generator.StructureGenerators.minimalProgram
import bestetti.enzo.smlgen.sml.generator.StructureGenerators.simpleProgram
import kotlin.random.Random

/**
 * Complete SML program generator with configurable complexity and length.
 */
object SmlProgramGenerator {
    /**
     * Generate a valid SML program with the given configuration.
     *
     * The generated program will have at most [ProgramConfig.maxLength] characters.
     * If initial generation exceeds the limit, the generator will retry with
     * reduced complexity settings.
     *
     * @param config Configuration for program generation
     * @return A valid SML program string with length <= config.maxLength
     */
    fun generate(config: ProgramConfig): String {
        var currentComplexity = config.complexity
        var attempts = 0
        val maxAttempts = 10
        while (attempts < maxAttempts) {
            val genConfig = currentComplexity.toConfig()
            val ctx = GenerationContext(random = Random(config.seed + attempts), config = genConfig)
            val generator = selectGenerator(config.copy(complexity = currentComplexity))

            val result = generator(ctx) //Invoke the generator

            if (result.length <= config.maxLength) {
                return result
            }
            // Try lower complexity
            currentComplexity = when (currentComplexity) {
                ProgramComplexity.EXTREME -> ProgramComplexity.COMPLEX
                ProgramComplexity.COMPLEX -> ProgramComplexity.MEDIUM
                ProgramComplexity.MEDIUM -> ProgramComplexity.SIMPLE
                ProgramComplexity.SIMPLE -> ProgramComplexity.MINIMAL
                ProgramComplexity.MINIMAL -> break
            }
            attempts++
        }

        // Fallback: generate minimal.
        val genConfig = ProgramComplexity.MINIMAL.toConfig()
        val ctx = GenerationContext(random = Random(config.seed), config = genConfig)
        return minimalProgram(ctx)
    }

    /**
     * Generate multiple programs with varying complexity.
     *
     * @param count Number of programs to generate
     * @param maxLength Maximum length for each program
     * @param seed Base seed for reproducibility
     * @return List of generated SML programs
     */
    fun generateTestSuite(count: Int, maxLength: Int, seed: Long = Random.nextLong()): List<String> {
        val complexities = ProgramComplexity.entries
        return (0 until count).map { i ->
            val complexity = complexities[i % complexities.size]
            generate(
                ProgramConfig(
                    maxLength = maxLength,
                    complexity = complexity,
                    seed = seed + i,
                    includeComments = i % 3 != 0,  // Some without comments
                    includeObscureFeatures = i % 2 == 0  // Half with obscure features
                )
            )
        }
    }
    
    /** Select generator based on complexity */
    private fun selectGenerator(config: ProgramConfig): Generator = when (config.complexity) {
        ProgramComplexity.MINIMAL -> minimalProgram
        ProgramComplexity.SIMPLE -> simpleProgram
        ProgramComplexity.MEDIUM -> mediumProgram(config)
        ProgramComplexity.COMPLEX -> complexProgram(config)
        ProgramComplexity.EXTREME -> extremeProgram(config)
    }
}