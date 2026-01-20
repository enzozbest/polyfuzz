package rexp

import value.Empty
import value.Left
import value.Rec
import value.Right
import value.Seq
import value.Value

/**
 * Rectification functions used in the lexing process.
 * The idea is that derivatives of regular expressions can grow too big and must be simplified if possible.
 * However, the simplification would destroy matching information needed for lexing, so we use the functions defined
 * here to "undo" the simplification once in the injecting phase.
 *
 * The definitions for these functions were described (and proved correct) by Ausaf et al. in 2016.
 **/
object RectificationFunctions {
    /** Identity rectification function. */
    val id: (Value) -> Value = { v -> v }

    /** Wrap rectified value in [Right]. */
    fun right(f: (Value) -> Value): (Value) -> Value = { v -> Right(f(v)) }

    /** Wrap rectified value in [Left]. */
    fun left(f: (Value) -> Value): (Value) -> Value = { v -> Left(f(v)) }

    /** Rectification for alternation. */
    fun alt(
        f1: (Value) -> Value,
        f2: (Value) -> Value,
    ): (Value) -> Value =
        { v ->
            when (v) {
                is Right -> Right(f2(v.v))
                is Left -> Left(f1(v.v))
                else -> throw IllegalArgumentException("F_ALT expects Left or Right")
            }
        }

    /** Rectification for concatenation. */
    fun seq(
        f1: (Value) -> Value,
        f2: (Value) -> Value,
    ): (Value) -> Value =
        { v ->
            when (v) {
                is Seq -> Seq(f1(v.v1), f2(v.v2))
                else -> throw IllegalArgumentException("F_SEQ expects Sequ")
            }
        }

    /** Rectification for concatenation when left simplifies to epsilon. */
    fun seqEmpty1(
        f1: (Value) -> Value,
        f2: (Value) -> Value,
    ): (Value) -> Value =
        { v ->
            Seq(f1(Empty), f2(v))
        }

    /** Rectification for concatenation when right simplifies to epsilon. */
    fun seqEmpty2(
        f1: (Value) -> Value,
        f2: (Value) -> Value,
    ): (Value) -> Value =
        { v ->
            Seq(f1(v), f2(Empty))
        }

    /** Rectification inside a recorded value. */
    fun recd(f: (Value) -> Value): (Value) -> Value =
        { v ->
            when (v) {
                is Rec -> Rec(v.x, f(v.v))
                else -> throw IllegalArgumentException("F_RECD expects Rec")
            }
        }

    /** Sentinel for impossible rectifications; should never be invoked. */
    val ERROR: (Value) -> Value = { throw Exception("THIS FUNCTION SHOULD NEVER BE CALLED!") }
}
