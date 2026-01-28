package bestetti.enzo.smlgen.sml.generator

import kotlin.random.Random

/**
 * Configuration for program generation.
 */
data class ProgramConfig(
    val maxLength: Int,
    val complexity: ProgramComplexity = ProgramComplexity.MEDIUM,
    val seed: Long = Random.nextLong(), //Random seed for reproducible generation
    val includeComments: Boolean = true,
    val includeObscureFeatures: Boolean = true
)