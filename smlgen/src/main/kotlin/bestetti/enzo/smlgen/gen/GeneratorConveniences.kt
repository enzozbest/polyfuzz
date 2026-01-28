package bestetti.enzo.smlgen.gen

object GeneratorConveniences {
    val wrapParens: ((String) -> String) = {s ->  "($s)" }
    val wrapBrackets: ((String) -> String) = {s ->  "[$s]" }
    val wrapBraces: ((String) -> String) = {s ->  "{ $s }" }
}