@file:Suppress("ktlint:standard:no-wildcard-imports")

package test

import lexer.Verilex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import rexp.*

class VerilexEndToEndTest {
    @Test
    fun tokenizeIdentifiersAndOps_star() {
        val letters = RANGE(('a'..'z').toSet())
        val id = "id" T (letters F letters.S())
        val op = "op" T ("+" X "-" X "*" X "/")
        val lexerRegex = (op X id).S().toCharFunctionFormat()

        val input = "sum+a-b/c"
        val tokens = Verilex.lex(lexerRegex, input)
        print(tokens)
        val expected =
            listOf(
                "id" to "sum",
                "op" to "+",
                "id" to "a",
                "op" to "-",
                "id" to "b",
                "op" to "/",
                "id" to "c",
            )
        assertEquals(expected, tokens)
    }

    @Test
    fun tokenizePlusAndStar_paths() {
        val a = "a".toRegex()
        val plus = "A" T a.P()
        val star = "S" T a.S()
        val r = (plus F star).toCharFunctionFormat()

        val tokens = Verilex.lex(r, "aaaa")
        // one or more -> first token is at least one 'a'; star can be the rest (including empty)
        val expected = listOf("A" to "aaaa", "S" to "")
        assertEquals(expected, tokens)
    }

    @Test
    fun errorWhenRegexCannotConsumeAllInput() {
        val r = ("tag" T "a").toCharFunctionFormat()
        // Provide empty input: r isn't nullable when input is empty after consuming nothing
        assertThrows(IllegalStateException::class.java) {
            Verilex.lex(r, "")
        }
    }
}
