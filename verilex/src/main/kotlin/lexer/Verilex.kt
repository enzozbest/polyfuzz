package lexer

import rexp.P
import rexp.RANGE
import rexp.RegularExpression
import rexp.S
import rexp.T
import rexp.X
import value.Value

/**
 * Verilex — formally verified derivative-based lexer described by Sulzmann & Lu, and Ausaf et al.
 *
 * This object exposes a small API for running a lexer defined as a
 * [RegularExpression] over an input [String]. The implementation follows
 * Brzozowski derivatives: it consumes one character at a time (`der`),
 * simplifies the resulting regex (`simp`) while collecting a rectification
 * function, and finally “injects” the character back into the value structure
 * via [Injector]. At the end, the accumulated [Value] is converted into
 * a list of `(tag, lexeme)` pairs using [Value.env].
 */
object Verilex {
    /**
     * Internal recursive driver that walks the input and builds a [Value]
     * representation of the match. When the list is empty, it either returns
     * the epsilon value (`mkeps`) if the regex is nullable, or throws.
     */
    private fun lexInternal(r: RegularExpression, s: List<Char>): Value = when {
            s.isEmpty() -> if(r.nullable()) r.mkeps() else error("Lexing error: nullable called on $r")
            else -> {
                val c = s[0]
                val cs = s.drop(1)
                val (rSimp, fSimp) = r.der(c).simp()
                Injector.inj(r, c, fSimp(lexInternal(rSimp, cs)))
            }
        }
    /**
     * Run the lexer described by [r] over [s], returning the tagged tokens as
     * a list of `(tag, lexeme)` pairs. The tags are introduced using `"name" T r`.
     */
    fun lex(r: RegularExpression, s: String): List<Pair<String, String>> = lexInternal(r, s.toList()).env()
}
