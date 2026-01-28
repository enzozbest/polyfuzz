package bestetti.enzo.smlgen.gen

/**
 * A collection of combinators for constructing complex [Generator] instances by combining or modifying simpler ones.
 *
 * This object provides a variety of utility functions for creating sequences, weighted choices, recursive structures,
 * and other flexible generation patterns.
 */
object GeneratorCombinators {
    /**
     * Combines multiple generators into a single generator that produces a concatenation of their outputs in order.
     *
     * @param gens A variable number of generators.
     * @return A new generator that produces a single string by concatenating the outputs of the provided generators.
     */
    fun seq(vararg gens: Generator): Generator =
        Generator { ctx ->
            buildString { for (g in gens) append(g.generate(ctx)) }
        }

    /**
     * Creates a generator that randomly selects one of the provided generators to produce a value.
     *
     * @param gens A variable number of generators.
     * @return A new generator that produces a value by invoking one of the given generators, selected uniformly at random.
     */
    fun choice(vararg gens: Generator): Generator =
        Generator { ctx ->
            gens[ctx.random.nextInt(gens.size)].generate(ctx)
        }

    /**
     * Selects a generator based on weighted probabilities and invokes it to generate a string.
     *
     * @param options A variable number of pairs where the first element is a positive weight indicating the likelihood
     * of selecting the associated generator, and the second element is the generator to be selected.
     * @return A new generator that selects one of the provided generators based on their respective weights
     * @throws IllegalArgumentException if no options are provided or if the total weight is less than or equal to zero.
     */
    fun weightedChoice(vararg options: Pair<Double, Generator>): Generator {
        require(options.isNotEmpty()) { "Must have at least one option!" }
        val totalWeight = options.sumOf { it.first }
        require(totalWeight > 0.0) { "Total weight must be positive!" }
        return Generator { ctx ->
            var roll = ctx.random.nextDouble() * totalWeight
            for ((weight, gen) in options) {
                roll -= weight
                if (roll <= 0.0) return@Generator gen.generate(ctx)
            }
            options.last().second.generate(ctx)
        }
    }

    /**
     * Creates a generator that dynamically chooses between two provided generators based on the current depth and
     * maximum depth in the generation context.
     *
     * If the current depth has reached the maximum defined in the context, the [terminal] generator is always used.
     * Otherwise, a probability determines whether to use the [recursive] generator or the [terminal] one.
     *
     * @param terminal The generator to use when the maximum depth is reached or when a probability favours the terminal path.
     * @param recursive The generator to use when under the maximum depth and the probability favours the recursive path.
     * @return A generator that produces output by alternately using the [terminal] or [recursive] generators.
     */
    fun depthChoice(terminal: Generator, recursive: Generator): Generator =
        Generator { ctx ->
            if (ctx.isAtMaxDepth) {
                terminal.generate(ctx)
            } else {
                val p = ctx.recursiveProbability()
                if (ctx.random.nextDouble() < p) {
                    ctx.deeper { recursive.generate(ctx) }
                } else {
                    terminal.generate(ctx)
                }
            }
        }

    /**
     * Chooses a generation strategy based on the current depth of the generation context.
     *
     * If the current depth is at the maximum allowed, a terminal generator is selected. Otherwise, a recursive generator
     * may be chosen based on a probability.
     *
     * @param terminals A list of non-recursive generators to choose from.
     * @param recursives A list of recursive generators to choose from.
     * @return A new generator that decides between terminal and recursive generation strategies.
     * @throws IllegalArgumentException If the terminals list or the recursives list is empty.
     */
    fun depthChoice(terminals: List<Generator>, recursives: List<Generator>): Generator {
        require(terminals.isNotEmpty()) { "Must have at least one terminal!" }
        require(recursives.isNotEmpty()) { "Must have at least one recursive option!" }
        return Generator { ctx ->
            if (ctx.isAtMaxDepth) {
                terminals[ctx.random.nextInt(terminals.size)].generate(ctx)
            } else {
                val p = ctx.recursiveProbability()
                if (ctx.random.nextDouble() < p) {
                    ctx.deeper {
                        recursives[ctx.random.nextInt(recursives.size)].generate(ctx)
                    }
                } else {
                    terminals[ctx.random.nextInt(terminals.size)].generate(ctx)
                }
            }
        }
    }

    /**
     * Creates a generator that produces a string by concatenating the outputs of the given generator
     * a random number of times. The number of repetitions is determined by the random value generated within
     * the range [0, maxRepeat] from the configuration of the generation context.
     *
     * @param gen The generator whose output will be repeatedly generated and concatenated.
     * @return A new generator that produces a concatenated string of repeated outputs from the input generator.
     */
    fun many(gen: Generator): Generator =
        Generator { ctx ->
            val n = ctx.random.nextInt(ctx.config.maxRepeat + 1)
            buildString { repeat(n) { append(gen.generate(ctx)) } }
        }

    /**
     * Creates a generator that produces a string by repeating the output of the provided generator
     * at least once and up to a maximum number of times, as determined by the generation context's configuration.
     *
     * @param gen The generator whose output will be repeated.
     * @return A new generator that produces a string consisting of repeated outputs of the given generator.
     */
    fun many1(gen: Generator): Generator =
        Generator { ctx ->
            val n = 1 + ctx.random.nextInt(ctx.config.maxRepeat)
            buildString { repeat(n) { append(gen.generate(ctx)) } }
        }

    /**
     * Creates a generator that optionally produces a value based on the provided generator.
     *
     * This generator uses randomness to decide whether to produce a value from the given generator or to produce an
     * empty string based on a coin flip.
     *
     * @param gen The generator to optionally invoke for value generation.
     * @return A new generator that produces either an empty string or a result from the given generator.
     */
    fun optional(gen: Generator): Generator =
        Generator { ctx ->
            if (ctx.random.nextBoolean()) gen.generate(ctx) else ""
        }

    /**
     * Creates a generator that produces a string by repeatedly invoking the provided generator exactly
     * [n] times and concatenating the results.
     *
     * @param n The number of times to invoke the given generator. Must be non-negative.
     * @param gen The generator to be invoked repeatedly to produce the concatenated output.
     * @return A new generator that produces a string consisting of [n] concatenated outputs of the given generator.
     */
    fun count(n: Int, gen: Generator): Generator =
        Generator { ctx ->
            require(n > 0) { "n must be non-negative!" }
            buildString { repeat(n) { append(gen.generate(ctx)) } }
        }

    /**
     * Creates a generator that produces outputs by interleaving the results of the given generator
     * [gen] with the results of the separator generator [sep].
     * The number of repetitions is selected randomly between 0 and the configured maximum repeat value.
     * If the number of repetitions is 0, an empty string is returned.
     *
     * @param gen The generator used to produce the main content.
     * @param sep The generator used to produce the separator between the content produced by [gen].
     * @return A new generator that produces a string by alternating the outputs of [gen] and [sep].
     */
    fun sepBy(gen: Generator, sep: Generator): Generator =
        Generator { ctx ->
            val n = ctx.random.nextInt(ctx.config.maxRepeat + 1)
            if (n == 0) return@Generator ""
            buildString {
                append(gen.generate(ctx))
                repeat(n - 1) {
                    append(sep.generate(ctx))
                    append(gen.generate(ctx))
                }
            }
        }

    /**
     * Creates a generator that produces a sequence of strings generated by the provided [gen],
     * separated by the strings generated by the [sep], with at least one occurrence of [gen].
     *
     * The number of repetitions is determined randomly up to the [maxRepeat] in the configuration
     * provided by generation context.
     *
     * @param gen The generator responsible for producing the main strings in the sequence.
     * @param sep The generator responsible for producing the separators.
     * @return A new generator that produces a sequence of strings with at least one [gen] output,
     *         separated by [sep] outputs, repeated a random number of times.
     */
    fun sepBy1(gen: Generator, sep: Generator): Generator =
        Generator { ctx ->
            val n = 1 + ctx.random.nextInt(ctx.config.maxRepeat)
            buildString {
                append(gen.generate(ctx))
                repeat(n - 1) {
                    append(sep.generate(ctx))
                    append(gen.generate(ctx))
                }
            }
        }

    /**
     * Creates a new generator that wraps the output of the current generator between the outputs
     * of the specified [before] and [after] generators.
     *
     * @param before The generator whose output will be prefixed to the result of the current generator.
     * @param after The generator whose output will be suffixed to the result of the current generator.
     * @return A new generator that produces a string by combining the output of the [before] generator, the output of
     * the current generator, and the output of the [after] generator in sequence.
     */
    fun Generator.between(
        before: Generator,
        after: Generator,
    ): Generator = seq(before, this, after)

    /**
     * Creates a generator that lazily evaluates and generates content using the provided supplier function.
     *
     * The supplied function is invoked each time the generator is used, allowing for dynamic and deferred generation
     * of content.
     *
     * @param supplier A function that supplies a new instance of a generator when invoked.
     * @return A new generator that delegates to the generator produced by the supplier function.
     */
    fun lazy(supplier: () -> Generator): Generator = Generator { ctx -> supplier().generate(ctx) }

    /**
     * Transforms the output of this generator by applying the given transformation function to it.
     *
     * @param f A transformation function that takes the string output generated by this generator
     *          and returns a modified string.
     * @return A new generator that produces transformed strings based on the output of this generator.
     */
    fun Generator.map(f: (String) -> String): Generator = Generator { ctx -> f(this.generate(ctx)) }

    // Convenience extension functions for wrapping generator outputs
    fun Generator.wrapParens(): Generator =  this.map(GeneratorConveniences.wrapParens)
    fun Generator.wrapBrackets(): Generator = this.map(GeneratorConveniences.wrapBrackets)
    fun Generator.wrapBraces(): Generator =   this.map(GeneratorConveniences.wrapBraces)
}

