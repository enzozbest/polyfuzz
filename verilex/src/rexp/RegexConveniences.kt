package rexp

/**
 * Small DSL for writing regular expressions more easily.
 * The operations are X for choice/alternation (a|b), F for sequence (a b), S for Kleene-star (r*), P for plus (r+)
 * and T for tagging. For example, the regular expression "lowercase letter followed by any number of lowercase letters"
 * can be written as
 *
 * val letters = RANGE(('a'..'z').toSet())
 * val r = letters F letters.S()
 *
 * It can also be tagged with an identifier, e.g. "name":
 *
 * val tagged = "name" T r
 */

/** Convert a list of characters into a concatenated regex. */
fun charlist2rexp(s: List<Char>): RegularExpression = when {
    s.isEmpty() -> ONE
    s.size == 1 -> CHAR(s.first())
    else -> SEQ(CHAR(s.first()), charlist2rexp(s.drop(1)))
}

/** Convert a Kotlin [String] into a [RegularExpression] of its characters. */
fun String.toRegex() = charlist2rexp(this.toList())

/** Alternation (choice). */
infix fun RegularExpression.X(other: RegularExpression): RegularExpression = ALT(this, other)
/** Alternation (choice) with a [String] operand. */
infix fun RegularExpression.X(other: String): RegularExpression = ALT(this, other.toRegex())
/** Alternation (choice). */
infix fun String.X(other: RegularExpression): RegularExpression = this.toRegex() X other
infix fun String.X(other: String) = this X other.toRegex()

/** Concatenation (sequence). */
infix fun RegularExpression.F(other: RegularExpression): RegularExpression = SEQ(this, other)
/** Concatenation (sequence) with a [String] operand. */
infix fun RegularExpression.F(other: String): RegularExpression = SEQ(this, other.toRegex())
/** Concatenation (sequence). */
infix fun String.F(other: RegularExpression): RegularExpression = this.toRegex() F other
infix fun String.F(other: String) = this F other.toRegex()

/** Kleene star. */
fun RegularExpression.S(): RegularExpression = STAR(this)
fun String.S(): RegularExpression = this.toRegex().S()

/** Kleene plus. */
fun RegularExpression.P(): RegularExpression = PLUS(this)
fun String.P(): RegularExpression = this.toRegex().P()

/** Tag (record) a match of [r] under this [String] name. */
infix fun String.T(r: RegularExpression) = RECD(this, r)
infix fun String.T(r: String) = RECD(this, r.toRegex())
