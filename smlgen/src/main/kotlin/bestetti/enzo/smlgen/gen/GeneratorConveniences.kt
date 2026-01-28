package bestetti.enzo.smlgen.gen

object GeneratorConveniences {
    val wrapParens: ((String) -> String) = {s ->  "($s)" }
    val wrapBrackets: ((String) -> String) = {s ->  "[$s]" }
    val wrapBraces: ((String) -> String) = {s ->  "{ $s }" }

    fun String.toGenerator() = AtomicGenerators.literal(this)
    infix fun String.X (other: Generator): Generator = this.toGenerator() X other
    infix fun Generator.X (other: String): Generator = this X other.toGenerator()
    infix fun String.X (other: String): Generator = this.toGenerator() X other.toGenerator()
    infix fun String.F (other: Generator): Generator = this.toGenerator() F other
    infix fun Generator.F (other: String): Generator = this F other.toGenerator()
    infix fun String.F (other: String): Generator = this.toGenerator() F other.toGenerator()
}