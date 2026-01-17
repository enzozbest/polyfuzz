package test

import lexer.Injector
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import rexp.*
import value.Empty

class RegexConveniencesAndInjectorErrorTest {
    @Test
    fun dslOperatorsWorkWithStringsAndRexp() {
        val a = "a".toRegex()
        val b = "b".toRegex()
        val alt1 = a X b
        val alt2 = "a" X b
        val alt3 = a X "b"
        val alt4 = "a" X "b"
        val seq1 = a F b
        val seq2 = "a" F b
        val seq3 = a F "b"
        val seq4 = "a" F "b"
        val star = "a".S()
        val plus = "a".P()
        val tag = "t" T ("a")

        // Just ensure these constructs produce non-null rexp nodes and lower properly
        listOf(alt1, alt2, alt3, alt4, seq1, seq2, seq3, seq4, star, plus, tag).forEach {
            it.toCharFunctionFormat()
        }
    }

    @Test
    fun injectorElseBranchThrows() {
        // Choose r that matches none of the handled cases in Injector.injInternal
        val r: RegularExpression = ONE
        assertThrows(IllegalArgumentException::class.java) {
            Injector.inj(r, 'x', Empty)
        }
    }
}
