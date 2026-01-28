package bestetti.enzo.smlgen.gen

/**
 * Provides a collection of utility functions to generate atomic string values using the [Generator] interface.
 */
object AtomicGenerators {

    /**
     * Creates a generator that always produces the given literal string.
     *
     * @param s The string to be produced by the generator.
     * @return A generator that produces the provided string.
     */
    fun literal(s: String): Generator = Generator { s }

    /**
     * Creates a generator that always produces an empty string.
     *
     * @return A generator that generates an empty string when invoked.
     */
    fun empty(): Generator = Generator { "" }

    /**
     * Creates a generator that produces a single whitespace character (" ").
     *
     * @return A generator that generates a string containing a single space.
     */
    fun ws(): Generator = Generator { " " }

    /**
     * Creates a generator that randomly selects one string from the provided list of strings.
     *
     * @param strings The list of strings to choose from.
     * @return A generator that produces one of the given strings selected uniformly at random.
     */
    fun oneOf(vararg strings: String): Generator =
        Generator { ctx ->
            strings[ctx.random.nextInt(strings.size)]
        }
}