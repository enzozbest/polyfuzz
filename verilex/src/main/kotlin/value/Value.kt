package value

/**
 * Value algebra used during lexing (Sulzmann–Lu style).
 *
 * Values accumulate match structure while the derivative engine consumes input.
 * At the end of lexing, [env] flattens the tree into a list of `(tag, lexeme)`
 * pairs, and [flatten] produces the raw matched text.
 */
sealed class Value {
    /** Render the raw matched text represented by this value. */
    abstract fun flatten(): String

    /** Extract tagged tokens `(tag, lexeme)` from this value. */
    abstract fun env(): List<Pair<String, String>>
}

/** Epsilon match (empty string). */
data object Empty: Value() {
    override fun flatten(): String = ""
    override fun env(): List<Pair<String, String>> = emptyList()
}

/** Single matched character. */
data class Chr(val c: Char): Value() {
    override fun flatten(): String = c.toString()
    override fun env(): List<Pair<String, String>> = emptyList()
}

/** Left branch of alternation. */
data class Left(val v: Value): Value() {
    override fun flatten(): String = v.flatten()
    override fun env(): List<Pair<String, String>> = v.env()
}

/** Right branch of alternation. */
data class Right(val v: Value):  Value() {
    override fun flatten(): String = v.flatten()
    override fun env(): List<Pair<String, String>> = v.env()
}

/** Concatenation of two values. */
data class Seq(val v1: Value, val v2: Value): Value() {
    override fun flatten(): String = v1.flatten() + v2.flatten()
    override fun env(): List<Pair<String, String>> = v1.env() + v2.env()
}
/** Zero or more repetitions. */
data class Stars(val vs: List<Value>): Value() {
    override fun flatten(): String = vs.joinToString("") { it.flatten() }
    override fun env(): List<Pair<String, String>> = vs.flatMap { it.env() }
}

/** One or more repetitions. */
data class Plus(val vs: List<Value>): Value() {
    override fun flatten(): String = vs.joinToString("") { it.flatten() }
    override fun env(): List<Pair<String, String>> = vs.flatMap { it.env() }
}

/** Recorded/tagged value under name [x]. */
data class Rec(val x: String, val v: Value): Value() {
    override fun flatten(): String = v.flatten()
    override fun env(): List<Pair<String, String>> = listOf(x to v.flatten())
}

