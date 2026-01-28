package bestetti.enzo.smlgen.sml.generator

import bestetti.enzo.smlgen.sml.generator.SmlProgramGenerator.generate
import kotlin.random.Random

object SmlProgramGeneratorConveniences {
    /**
     * Generate a single minimal SML program.
     */
    fun generateMinimal(maxLength: Int = 50, seed: Long = Random.nextLong()): String =
        generate(ProgramConfig(maxLength, ProgramComplexity.MINIMAL, seed))

    /**
     * Generate a single simple SML program.
     */
    fun generateSimple(maxLength: Int = 100, seed: Long = Random.nextLong()): String =
        generate(ProgramConfig(maxLength, ProgramComplexity.SIMPLE, seed))

    /**
     * Generate a single medium complexity SML program.
     */
    fun generateMedium(maxLength: Int = 500, seed: Long = Random.nextLong()): String =
        generate(ProgramConfig(maxLength, ProgramComplexity.MEDIUM, seed))

    /**
     * Generate a single complex SML program.
     */
    fun generateComplex(maxLength: Int = 1000, seed: Long = Random.nextLong()): String =
        generate(ProgramConfig(maxLength, ProgramComplexity.COMPLEX, seed))

    /**
     * Generate a single extreme complexity SML program with obscure features.
     */
    fun generateExtreme(maxLength: Int = 2000, seed: Long = Random.nextLong()): String =
        generate(ProgramConfig(maxLength, ProgramComplexity.EXTREME, seed, includeObscureFeatures = true))

}