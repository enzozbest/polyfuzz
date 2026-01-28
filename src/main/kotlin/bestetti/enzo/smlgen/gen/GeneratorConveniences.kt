package bestetti.enzo.smlgen.gen

import bestetti.enzo.smlgen.gen.GeneratorCombinators.map

object GeneratorConveniences {
    val wrapParens: ((String) -> String) = {s ->  "($s)" }
    val wrapBrackets: ((String) -> String) = {s ->  "[$s]" }
    val wrapBraces: ((String) -> String) = {s ->  "{ $s }" }
}