package bestetti.enzo.smlgen

import bestetti.enzo.smlgen.sml.generator.ProgramComplexity

sealed interface Complexity {
    data class Single(val level: ProgramComplexity) : Complexity
    data object Mixed : Complexity
}

/**
 * Command-line arguments for smlgen.
 *
 * Supported flags:
 *   -n <count>       Number of test files to generate (default: 10)
 *   -seed <seed>     Seed for reproducible generation (random if omitted)
 *   -o <dir>         Output directory (default: current directory)
 *   -c <complexity>  Complexity level (default: MIXED)
 *                    Values: MINIMAL, SIMPLE, MEDIUM, COMPLEX, EXTREME, MIXED
 *   -h, --help       Show usage information
 */
data class CliArgs(
    val count: Int = 10,
    val seed: Long? = null,
    val outputDir: String = ".",
    val complexity: Complexity = Complexity.Mixed,
) {
    companion object {
        private val validComplexities =
            ProgramComplexity.entries.map { it.name } + "MIXED"

        fun parse(args: Array<String>): CliArgs {
            var count = 10
            var seed: Long? = null
            var outputDir = "."
            var complexity: Complexity = Complexity.Mixed

            val iter = args.iterator()
            while (iter.hasNext()) {
                when (val flag = iter.next()) {
                    "-n" -> count = iter.nextValue(flag).toIntOrNull()
                        ?.also { require(it > 0) { "Count must be positive, got $it" } }
                        ?: error("Invalid number for -n")

                    "-seed" -> seed = iter.nextValue(flag).toLongOrNull()
                        ?: error("Invalid seed value")

                    "-o" -> outputDir = iter.nextValue(flag)

                    "-c" -> {
                        val value = iter.nextValue(flag).uppercase()
                        complexity = when (value) {
                            "MIXED" -> Complexity.Mixed
                            else -> Complexity.Single(
                                ProgramComplexity.entries.find { it.name == value }
                                    ?: error("Unknown complexity '$value'. Valid: ${validComplexities.joinToString()}")
                            )
                        }
                    }

                    "-h", "--help" -> {
                        printUsage()
                        throw HelpRequestedException()
                    }

                    else -> error("Unknown argument: $flag\nRun with -h for usage.")
                }
            }

            return CliArgs(count, seed, outputDir, complexity)
        }

        private fun Iterator<String>.nextValue(flag: String): String {
            require(hasNext()) { "Missing value for $flag" }
            return next()
        }

        fun printUsage() {
            println(
                """
                |Usage: smlgen [-n <count>] [-seed <seed>] [-o <dir>] [-c <complexity>]
                |
                |Options:
                |  -n <count>       Number of test files to generate (default: 10)
                |  -seed <seed>     Seed for reproducible generation (random if omitted)
                |  -o <dir>         Output directory (default: current directory)
                |  -c <complexity>  Complexity level (default: MIXED)
                |                   Values: MINIMAL, SIMPLE, MEDIUM, COMPLEX, EXTREME, MIXED
                |  -h, --help       Show this help message
                """.trimMargin()
            )
        }
    }
}

/**
 * Exception thrown when the user requests help (e.g., with -h or --help).
 * This is used to signal that the program should exit after displaying usage information.
 */
class HelpRequestedException : Exception()
