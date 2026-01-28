package bestetti.enzo.smlgen.gen

import bestetti.enzo.smlgen.gen.GeneratorCombinators.choice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.seq

/**
 * Represents a functional interface for generating strings based on the supplied generation context.
 *
 * Additionally, provides a small DSL for combining generators using infix functions.
 */
fun interface Generator {
    /**
     * Generates a string based on the given generation context.
     *
     * @param ctx The generation context containing configuration, randomness, and depth tracking
     *            to guide the generation process.
     * @return The generated string.
     */
    fun generate(ctx: GenerationContext): String

    /**
     * Combines this generator with another generator to produce a sequence of their outputs.
     *
     * The resulting generator will concatenate the outputs of the current generator and the provided generator
     * in the order they are chained using this function.
     *
     * @param other The generator whose output will follow the output of the current generator.
     * @return A new generator that produces a sequence combining the outputs of both generators.
     */
    infix fun F(other: Generator): Generator = seq(this, other)

    /**
     * Combines two generators into a new generator that produces output by randomly selecting
     * one of the two generators to generate a value.
     *
     * @param other The generator to combine with the current generator.
     * @return A generator that randomly selects between the current generator and the provided generator.
     */
    infix fun X(other: Generator): Generator = choice(this, other)

    /**
     * Invokes the generator using the provided generation context and produces a string.
     *
     * @param ctx The generation context containing configuration and randomisation utilities used during generation.
     * @return The generated string based on the implementation of the generator.
     */
    operator fun invoke(ctx: GenerationContext): String = generate(ctx)
}
