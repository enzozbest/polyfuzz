package bestetti.enzo.smlgen.sml

import bestetti.enzo.smlgen.sml.generator.ProgramComplexity
import bestetti.enzo.smlgen.sml.generator.ProgramConfig
import bestetti.enzo.smlgen.sml.generator.SmlProgramGenerator
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
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
    //generateSuiteFiles(10)
    generateTestPrograms()
}

fun generateTestPrograms(cnt: Int = 10, maxLen: Int = 300, seed: Long = Random.nextLong()) {
    val testSuite = SmlProgramGenerator.generateTestSuite(count = cnt, maxLength = maxLen, seed = seed)
    testSuite.forEachIndexed { i, program ->
        println("Program $i (${program.length} chars):")
        println(program)
    }
}

fun generateSuiteFiles(cycles: Int = 10, path: String = "../polylex-harness/afl/corpus/") {
    for (i in 0 until cycles) {
        val seed = Random.nextLong()
        val testSuite = SmlProgramGenerator.generateTestSuite(count = 10, maxLength = 300, seed = seed)
        testSuite.forEachIndexed { j, prog ->
            val fileName = "Program_${j + 1}_${prog.length}_$seed.sml"
            val file = Path("$path/$fileName")
            file.createParentDirectories()
            file.writeText(prog)
        }
    }
}
