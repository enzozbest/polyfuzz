package tokenizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TokenisationErrorTest {
    @Test
    fun testTokenisationErrorCreation() {
        val error = TokenisationError(
            message = "Test error message",
            position = 42,
            context = "some context"
        )

        assertEquals("Test error message", error.message)
        assertEquals(42, error.position)
        assertEquals("some context", error.context)
        assertNull(error.cause)
    }

    @Test
    fun testTokenisationErrorWithCause() {
        val cause = RuntimeException("Original error")
        val error = TokenisationError(
            message = "Wrapped error",
            position = 10,
            context = "ctx",
            cause = cause
        )

        assertEquals("Wrapped error", error.message)
        assertEquals(10, error.position)
        assertEquals("ctx", error.context)
        assertSame(cause, error.cause)
    }

    @Test
    fun testTokenisationErrorToString() {
        val error = TokenisationError(
            message = "Unexpected character",
            position = 5,
            context = "hello world"
        )

        val expected = "TokenisationError at position 5: Unexpected character\n  Context: \"hello world\""
        assertEquals(expected, error.toString())
    }
}
