package bestetti.enzo.smlgen.gen

import kotlin.math.pow
import kotlin.random.Random

data class GenerationConfig(
    val maxDepth: Int = 10,
    val maxRepeat: Int = 5,
)

class GenerationContext(
    val random: Random = Random,
    val config: GenerationConfig = GenerationConfig(),
) {
    @PublishedApi
    internal var depth: Int = 0

    val isAtMaxDepth: Boolean
        get() = depth >= config.maxDepth

    /**
     * Returns a probability that decays smoothly from 1.0 at depth 0
     * to ~0.0 at maxDepth using exponential decay.
     */
    fun recursiveProbability(): Double {
        if (config.maxDepth <= 0) return 0.0
        val ratio = depth.toDouble() / config.maxDepth
        return (1.0 - ratio).pow(2.0)
    }

    inline fun <T> deeper(block: () -> T): T {
        depth++
        try {
            return block()
        } finally {
            depth--
        }
    }
}
