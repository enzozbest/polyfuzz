package bestetti.enzo.smlgen.sml.generator

import bestetti.enzo.smlgen.gen.GenerationConfig

/**
 * Complexity level for generated programs.
 */
enum class ProgramComplexity {
    /** Minimal programs: single declarations, simple expressions */
    MINIMAL,

    /** Simple programs: few declarations, basic expressions */
    SIMPLE,

    /** Medium programs: moderate nesting, variety of constructs */
    MEDIUM,

    /** Complex programs: deep nesting, advanced features */
    COMPLEX,

    /** Extreme programs: maximum nesting, edge cases, obscure features */
    EXTREME;

    fun toConfig() = when (this) {
        MINIMAL -> GenerationConfig(maxDepth = 2, maxRepeat = 1)
        SIMPLE -> GenerationConfig(maxDepth = 3, maxRepeat = 2)
        MEDIUM -> GenerationConfig(maxDepth = 5, maxRepeat = 3)
        COMPLEX -> GenerationConfig(maxDepth = 7, maxRepeat = 4)
        EXTREME -> GenerationConfig(maxDepth = 10, maxRepeat = 5)
    }
}