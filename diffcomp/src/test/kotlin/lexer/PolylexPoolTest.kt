package lexer

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylexPoolTest {

    private fun createScript(content: String): File {
        val script = File.createTempFile("polylex-pool-test-", ".sh")
        script.writeText("#!/bin/bash\n$content")
        script.setExecutable(true)
        script.deleteOnExit()
        return script
    }

    @Test
    fun `tokenise delegates to PolylexInvoker and returns Success`() = runBlocking {
        val script = createScript("cat > /dev/null\necho 'VAL'\necho 'ID(x)'\n")
        val pool = PolylexPool(binaryPath = script.absolutePath, poolSize = 1)
        val result = pool.tokenise("val x = 1")
        assertIs<LexerResult.Success>(result)
        assertEquals("VAL ID(x)", result.tokenStream)
    }

    @Test
    fun `tokenise propagates Failure from PolylexInvoker`() = runBlocking {
        val pool = PolylexPool(binaryPath = "/nonexistent/binary", poolSize = 1)
        val result = pool.tokenise("val x = 1")
        assertIs<LexerResult.Failure>(result)
        assertTrue(result.error.contains("invocation failed"))
    }
}
