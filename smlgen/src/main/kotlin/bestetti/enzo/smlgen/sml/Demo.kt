package bestetti.enzo.smlgen.sml

import bestetti.enzo.smlgen.sml.generator.ProgramComplexity
import bestetti.enzo.smlgen.sml.generator.ProgramConfig
import bestetti.enzo.smlgen.sml.generator.SmlProgramGenerator
import kotlin.random.Random

/**
 * Demonstration of the SML program generator.
 */
fun main() {
    println("=".repeat(60))
    println("SML Program Generator Demo")
    println("=".repeat(60))

    // Generate examples at each complexity level
    for (complexity in ProgramComplexity.entries) {
        println("\n--- ${complexity.name} ---")
        val maxLen = when (complexity) {
            ProgramComplexity.MINIMAL -> 50
            ProgramComplexity.SIMPLE -> 150
            ProgramComplexity.MEDIUM -> 400
            ProgramComplexity.COMPLEX -> 800
            ProgramComplexity.EXTREME -> 1500
        }

        repeat(3) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(
                    maxLength = maxLen,
                    complexity = complexity,
                    seed = Random.nextLong() * 100 + complexity.ordinal,
                    includeComments = complexity.ordinal > 1,
                    includeObscureFeatures = complexity == ProgramComplexity.EXTREME
                )
            )
            println("\nExample ${i + 1} (${program.length} chars):")
            println(program)
        }
    }

    println("\n" + "=".repeat(60))
    println("Test Suite Generation")
    println("=".repeat(60))

    val testSuite = SmlProgramGenerator.generateTestSuite(count = 10, maxLength = 300, seed = Random.nextLong())
    testSuite.forEachIndexed { i, prog ->
        println("\nProgram ${i + 1} (${prog.length} chars):")
        println(prog)
    }
}
