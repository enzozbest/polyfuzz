package lexer

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylexInvokerTest {

    private fun createScript(content: String): File {
        val script = File.createTempFile("polylex-test-", ".sh")
        script.writeText("#!/bin/bash\n$content")
        script.setExecutable(true)
        script.deleteOnExit()
        return script
    }

    @Test
    fun `invoke returns Failure when binary does not exist`() = runBlocking {
        val result = PolylexInvoker.invoke("/nonexistent/polylex_binary", "val x = 1")
        assertIs<LexerResult.Failure>(result)
        assertTrue(result.error.contains("invocation failed"), "Expected invocation failure, got: ${result.error}")
    }

    @Test
    fun `invoke returns Success for binary that outputs tokens`() = runBlocking {
        val script = createScript("cat > /dev/null\necho 'VAL'\necho 'ID(x)'\n")
        val result = PolylexInvoker.invoke(script.absolutePath, "val x = 1")
        assertIs<LexerResult.Success>(result)
        assertEquals("VAL ID(x)", result.tokenStream)
    }

    @Test
    fun `invoke returns Failure for non-zero exit code`() = runBlocking {
        val script = createScript("cat > /dev/null\necho 'error output' >&2\nexit 1\n")
        val result = PolylexInvoker.invoke(script.absolutePath, "val x = 1")
        assertIs<LexerResult.Failure>(result)
        assertTrue(result.error.contains("exited with code 1"), "Expected exit code in error, got: ${result.error}")
    }

    @Test
    fun `invoke returns Failure for blank output with zero exit code`() = runBlocking {
        val script = createScript("cat > /dev/null\nexit 0\n")
        val result = PolylexInvoker.invoke(script.absolutePath, "val x = 1")
        assertIs<LexerResult.Failure>(result)
        assertTrue(result.error.contains("empty output"), "Expected empty output error, got: ${result.error}")
    }

    @Test
    fun `invoke joins multi-line output with spaces`() = runBlocking {
        val script = createScript("cat > /dev/null\necho 'VAL'\necho 'ID(x)'\necho '='\necho 'INT(1)'\n")
        val result = PolylexInvoker.invoke(script.absolutePath, "val x = 1")
        assertIs<LexerResult.Success>(result)
        assertEquals("VAL ID(x) = INT(1)", result.tokenStream)
    }

    @Test
    fun `invoke trims trailing whitespace from output`() = runBlocking {
        // Script outputs a trailing newline (echo always adds one), which should be trimmed
        val script = createScript("cat > /dev/null\necho 'VAL ID(x)'\n")
        val result = PolylexInvoker.invoke(script.absolutePath, "val x = 1")
        assertIs<LexerResult.Success>(result)
        assertEquals("VAL ID(x)", result.tokenStream)
    }
}
