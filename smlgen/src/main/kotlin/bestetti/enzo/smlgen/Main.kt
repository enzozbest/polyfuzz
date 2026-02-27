package bestetti.enzo.smlgen

import bestetti.enzo.smlgen.sml.generator.ProgramComplexity
import bestetti.enzo.smlgen.sml.generator.ProgramConfig
import bestetti.enzo.smlgen.sml.generator.SmlProgramGenerator
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.random.Random


/**
 * Main entry point for smlgen.
 *
 * Usage:
 *   -n <count>       Number of programs to generate (default: 10)
 *   -seed <value>   Seed for random generation (default: random)
 *   -o <dir>        Output directory (default: current directory)
 *   -c <complexity> Complexity level (MINIMAL, SIMPLE, MEDIUM, COMPLEX, EXTREME, MIXED; default: MIXED)
 *   -h, --help      Show this help message
 */
fun main(args: Array<String>) {
    val config = try {
        CliArgs.parse(args)
    } catch (_: HelpRequestedException) {
        return
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        CliArgs.printUsage()
        return
    }

    val seed = config.seed ?: Random.nextLong().also { println("Using random seed: $it") }

    val outputDir = Path(config.outputDir).also { it.createDirectories() }

    val programs = when (val c = config.complexity) {
        is Complexity.Mixed -> generateMixed(config.count, seed)
        is Complexity.Single -> generateSingle(config.count, seed, c.level)
    }

    programs.forEachIndexed { i, program ->
        val fileName = "Program_${i + 1}_${program.length}_$seed.sml"
        outputDir.resolve(fileName).writeText(program)
        println("Generated: $fileName (${program.length} chars)")
    }

    println("Generated ${programs.size} file(s) in ${config.outputDir}")
}

/**
 * Quickly determines max length for each complexity level without hardcoding it in multiple places.
 */
private fun maxLengthFor(complexity: ProgramComplexity): Int = when (complexity) {
    ProgramComplexity.MINIMAL -> 50
    ProgramComplexity.SIMPLE -> 150
    ProgramComplexity.MEDIUM -> 400
    ProgramComplexity.COMPLEX -> 800
    ProgramComplexity.EXTREME -> 1500
}

/**
 * Generates a list of SML programs with mixed complexity levels.
 * The complexity of each program is determined by cycling through the available complexity levels.
 */
private fun generateMixed(count: Int, seed: Long): List<String> {
    val complexities = ProgramComplexity.entries
    return (0 until count).map { i ->
        val complexity = complexities[i % complexities.size]
        SmlProgramGenerator.generate(
            ProgramConfig(
                maxLength = maxLengthFor(complexity),
                complexity = complexity,
                seed = seed + i,
                includeComments = false,
                includeObscureFeatures = complexity == ProgramComplexity.EXTREME,
            )
        )
    }
}

/**
 * Generates a list of SML programs all at the specified complexity level.
 */
private fun generateSingle(count: Int, seed: Long, complexity: ProgramComplexity): List<String> {
    val maxLength = maxLengthFor(complexity)
    return (0 until count).map { i ->
        SmlProgramGenerator.generate(
            ProgramConfig(
                maxLength = maxLength,
                complexity = complexity,
                seed = seed + i,
                includeComments = false,
                includeObscureFeatures = complexity == ProgramComplexity.EXTREME,
            )
        )
    }
}
