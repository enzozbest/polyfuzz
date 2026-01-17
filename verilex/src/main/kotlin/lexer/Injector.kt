package lexer

import rexp.ALT
import rexp.CFUN
import rexp.PLUS
import rexp.RECD
import rexp.RegularExpression
import rexp.SEQ
import rexp.STAR
import value.Chr
import value.Left
import value.Plus
import value.Rec
import value.Right
import value.Seq
import value.Stars
import value.Value

object Injector {
    /**
     * Public injection entry point. Given the original regex [r], a consumed
     * character [c], and a partially built [Value] [v], produce a new [Value]
     * reflecting the match with [c] inserted at the correct position.
     */
    fun inj(r: RegularExpression, c: Char, v: Value): Value = when (v) {
        is Rec -> Rec(v.x, injInternal(r, c, v.v))
        else -> injInternal(r, c, v)
    }

    /**
     * Internal implementation that mirrors the cases needed for derivative‑
     * based lexing. See Sulzmann & Lu (2014), Urban (2016) for justification
     * of these cases.
     */
    private fun injInternal(r: RegularExpression, c: Char, v: Value): Value = when {
        v is Rec -> Rec(v.x, injInternal(r, c, v.v))
        r is STAR && v is Seq && v.v2 is Stars -> Stars(listOf(injInternal(r.r, c, v.v1)) + v.v2.vs)
        r is PLUS && v is Seq && v.v2 is Stars -> Plus(listOf(injInternal(r.r, c, v.v1)) + v.v2.vs)
        r is SEQ && v is Seq -> Seq(injInternal(r.r1, c, v.v1), v.v2)
        r is SEQ && v is Left && v.v is Seq -> Seq(injInternal(r.r1, c, v.v.v1), v.v.v2)
        r is SEQ && v is Right -> Seq(r.r1.mkeps(), injInternal(r.r2, c, v.v))
        r is ALT && v is Left -> Left(injInternal(r.r1, c, v.v))
        r is ALT && v is Right -> Right(injInternal(r.r2, c, v.v))
        r is CFUN -> Chr(c)
        r is RECD -> Rec(r.x, injInternal(r.r, c, v))
        else -> throw IllegalArgumentException("No match for r=$r, v=$v, c = $c")
    }
}