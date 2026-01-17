package rexp
import value.Empty
import value.Left
import value.Plus
import value.Rec
import value.Right
import value.Seq
import value.Stars
import value.Value

/**
 * Algebraic data type for regular expressions used by Verilex.
 *
 * This representation supports Brzozowski derivatives (`der`) and a
 * simplification step (`simp`) that yields a rectification function to recover
 * match structure after algebraic simplifications. See also
 * [toCharFunctionFormat], which lowers character constructors to predicate
 * matchers for efficient per‑character processing.
 */
sealed class RegularExpression {
    /**
     * @return true iff this regex matches the empty string; false otherwise.
     */
    abstract fun nullable(): Boolean

    /**
     * Compute the Brzozowski derivative with respect to character [c].
     */
    abstract fun der(c: Char): RegularExpression

    /**
     * Simplify this regex and return a rectification function to reconstruct
     * match structure during injection.
     *
     * @return a pair `(simplified, rectify)`.
     */
    abstract fun simp(): Pair<RegularExpression, (Value) -> Value>

    /**
     * Lower character constructors (`CHAR`, `RANGE`) into `CFUN` predicates.
     */
    abstract fun toCharFunctionFormat(): RegularExpression

    /**
     * Produce a [value.Value] witnessing a match of the empty string.
     *
     * @throws IllegalStateException if the regex is not nullable.
     */
    abstract fun mkeps(): Value
}

/** Matches nothing. */
data object ZERO : RegularExpression() {
    override fun toString(): String = "ZERO"
    override fun toCharFunctionFormat(): RegularExpression = this

    override fun nullable() = false
    override fun der(c: Char) = ZERO
    override fun simp(): Pair<RegularExpression, (Value) -> Value> = Pair(this, Rectification.id)

    override fun mkeps(): Value = error("mkeps not allowed on regex of type ZERO: $this")
}

/** Matches only the empty string. */
data object ONE : RegularExpression() {
    override fun toString(): String = "ONE"
    override fun toCharFunctionFormat(): RegularExpression = this

    override fun nullable() = true
    override fun der(c: Char) = ZERO
    override fun simp(): Pair<RegularExpression, (Value) -> Value> = Pair(this, Rectification.id)

    override fun mkeps(): Value = Empty
}

/** Single character literal; lowered to [CFUN] by [toCharFunctionFormat]. */
data class CHAR(val c: Char): RegularExpression() {
    override fun toString(): String = "[$c]"
    override fun toCharFunctionFormat(): RegularExpression = CFUN("[$c]") { c == it }
    override fun nullable() = error("CHAR type should not survive translation to CFUN")
    override fun der(c: Char) = error("CHAR type should not survive translation to CFUN")
    override fun simp() = error("CHAR type should not survive translation to CFUN")
    override fun mkeps() = error("CHAR type should not survive translation to CFUN")
}

/** Character class; lowered to [CFUN] by [toCharFunctionFormat]. */
data class RANGE(val cs: Set<Char>): RegularExpression() {
    override fun toString(): String = "[${cs.joinToString(",")}]"
    override fun toCharFunctionFormat(): RegularExpression = CFUN("[${cs.joinToString()}]") { cs.contains(it) }
    override fun nullable() = error("RANGE type should not survive translation to CFUN")
    override fun der(c: Char) = error("RANGE type should not survive translation to CFUN")
    override fun simp() = error("RANGE type should not survive translation to CFUN")
    override fun mkeps() = error("RANGE type should not survive translation to CFUN")
}

/** Predicate over characters used during lexing. */
data class CFUN(val asString: String, val f: (Char) -> Boolean) : RegularExpression() {
    override fun toString(): String = asString
    override fun toCharFunctionFormat(): RegularExpression = this

    override fun nullable() = false
    override fun der(c: Char) = if (f(c)) ONE else ZERO
    override fun simp(): Pair<RegularExpression, (Value) -> Value> = Pair(this, Rectification.id)

    override fun mkeps(): Value = error("mkeps not allowed on regex of type CFUN: $this")
}

/** Alternation (choice): `r1 | r2`. */
data class ALT(val r1: RegularExpression, val r2: RegularExpression) : RegularExpression() {
    override fun toString(): String = "($r1) | ($r2)"
    override fun toCharFunctionFormat(): RegularExpression = ALT(r1.toCharFunctionFormat(), r2.toCharFunctionFormat())
    override fun nullable() = r1.nullable() || r2.nullable()
    override fun der(c: Char) = ALT(r1.der(c), r2.der(c))
    override fun simp(): Pair<RegularExpression, (Value) -> Value> {
        val (r1s, f1s) = r1.simp()
        val (r2s, f2s) = r2.simp()
        return when {
            r1s == ZERO -> Pair(r2s, Rectification.right(f2s))
            r2s == ZERO -> Pair(r1s, Rectification.left(f1s))
            r1s == r2s -> Pair(r1s, Rectification.left(f1s))
            else -> Pair(ALT(r1s, r2s), Rectification.alt(f1s, f2s))
        }
    }
    override fun mkeps(): Value = if (r1.nullable()) Left(r1.mkeps()) else Right(r2.mkeps())
}

/** Concatenation (sequence): `r1 r2`. */
data class SEQ(val r1: RegularExpression, val r2: RegularExpression) : RegularExpression() {
    override fun toString(): String = "($r1) ~ ($r2)"
    override fun toCharFunctionFormat(): RegularExpression = SEQ(r1.toCharFunctionFormat(), r2.toCharFunctionFormat())

    override fun nullable() = r1.nullable() && r2.nullable()
    override fun der(c: Char) = if (r1.nullable()) {
        ALT(SEQ(r1.der(c), r2), r2.der(c))
    } else {
        SEQ(r1.der(c), r2)
    }
    override fun simp(): Pair<RegularExpression, (Value) -> Value> {
        val (r1s, f1s) = r1.simp()
        val (r2s, f2s) = r2.simp()
        return when {
            r1s == ZERO -> Pair(ZERO, Rectification.ERROR)
            r2s == ZERO -> Pair(ZERO, Rectification.ERROR)
            r1s == ONE -> Pair(r2s, Rectification.seq_Empty1(f1s, f2s))
            r2s == ONE -> Pair(r1s, Rectification.seq_Empty2(f1s, f2s))
            else -> Pair(SEQ(r1s, r2s), Rectification.seq(f1s, f2s))
        }
    }

    override fun mkeps(): Value = Seq(r1.mkeps(), r2.mkeps())
}

/** Kleene star: zero or more repetitions of [r]. */
data class STAR(val r : RegularExpression) : RegularExpression() {
    override fun toString(): String = "($r)*"
    override fun toCharFunctionFormat(): RegularExpression = STAR(r.toCharFunctionFormat())

    override fun nullable() = true
    override fun der(c: Char) = SEQ(r.der(c), STAR(r))
    override fun simp(): Pair<RegularExpression, (Value) -> Value> = Pair(this, Rectification.id)


    override fun mkeps(): Value = Stars(emptyList())
}

/** Kleene plus: one or more repetitions of [r]. */
data class PLUS(val r : RegularExpression) : RegularExpression() {
    override fun toString(): String = "($r)+"
    override fun toCharFunctionFormat(): RegularExpression = PLUS(r.toCharFunctionFormat())

    override fun nullable() = r.nullable()
    override fun der(c: Char) = SEQ(r.der(c), STAR(r))
    override fun simp(): Pair<RegularExpression, (Value) -> Value> = Pair(this, Rectification.id)

    override fun mkeps(): Value = Plus(listOf(r.mkeps()))
}

/** Tag (record) the match of [r] under name [x]. */
data class RECD(val x: String, val r: RegularExpression) : RegularExpression() {
    override fun toString(): String = "($x:$r)"
    override fun toCharFunctionFormat(): RegularExpression = RECD(x, r.toCharFunctionFormat())

    override fun nullable() = r.nullable()
    override fun der(c: Char) = r.der(c)
    override fun simp(): Pair<RegularExpression, (Value) -> Value> = Pair(this, Rectification.id)

    override fun mkeps(): Value = Rec(x, r.mkeps())
}
