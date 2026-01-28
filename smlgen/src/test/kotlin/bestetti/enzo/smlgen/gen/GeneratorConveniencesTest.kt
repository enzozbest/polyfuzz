package bestetti.enzo.smlgen.gen

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratorConveniencesTest {

    @Test
    fun `Test wrapParens wraps string in parentheses`() {
        val result = GeneratorConveniences.wrapParens("content")
        assertEquals("(content)", result)
    }

    @Test
    fun `Test wrapBrackets wraps string in brackets`() {
        val result = GeneratorConveniences.wrapBrackets("content")
        assertEquals("[content]", result)
    }

    @Test
    fun `Test wrapBraces wraps string in braces with spaces`() {
        val result = GeneratorConveniences.wrapBraces("content")
        assertEquals("{ content }", result)
    }
}
