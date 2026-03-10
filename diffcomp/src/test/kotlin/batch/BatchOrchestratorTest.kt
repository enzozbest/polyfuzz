package batch

import comparison.ComparisonResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import lexer.LexerResult
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BatchOrchestratorTest {

    private fun createTempSmlFile(dir: File, name: String, content: String): File =
        dir.resolve(name).also { it.writeText(content) }

    @Test
    fun `processes all files and returns one result per file`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-test").toFile()
        try {
            val files = listOf(
                createTempSmlFile(tempDir, "a.sml", "val x = 1"),
                createTempSmlFile(tempDir, "b.sml", "val y = 2"),
                createTempSmlFile(tempDir, "c.sml", "val z = 3"),
            )
            val tokens = "VAL ID(x) INT(1)"
            val oracle: (String) -> LexerResult = { LexerResult.Success(tokens, 0) }
            val polylex: suspend (String) -> LexerResult = { LexerResult.Success(tokens, 0) }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(3, results.size, "Should return one result per file")
            assertTrue(results.all { it is BatchFileResult.Success }, "All results should be Success")
            assertTrue(
                results.all { (it as BatchFileResult.Success).comparisonResult is ComparisonResult.Match },
                "All results should have Match (identical token streams)"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `captures oracle failure as BatchFileResult Failure`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-oracle-fail-test").toFile()
        try {
            val files = listOf(createTempSmlFile(tempDir, "a.sml", "val x = 1"))
            val oracle: (String) -> LexerResult = { LexerResult.Failure("oracle parse error") }
            val polylex: suspend (String) -> LexerResult = { LexerResult.Success("VAL ID(x) INT(1)", 0) }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(1, results.size, "Should return one result")
            assertIs<BatchFileResult.Failure>(results[0], "Should be Failure when oracle fails")
            assertTrue(
                (results[0] as BatchFileResult.Failure).error.contains("oracle"),
                "Error message should mention 'oracle'"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `captures polylex failure as BatchFileResult Failure`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-polylex-fail-test").toFile()
        try {
            val files = listOf(createTempSmlFile(tempDir, "a.sml", "val x = 1"))
            val oracle: (String) -> LexerResult = { LexerResult.Success("VAL ID(x) INT(1)", 0) }
            val polylex: suspend (String) -> LexerResult = { LexerResult.Failure("polylex binary error") }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(1, results.size, "Should return one result")
            assertIs<BatchFileResult.Failure>(results[0], "Should be Failure when polylex fails")
            assertTrue(
                (results[0] as BatchFileResult.Failure).error.contains("polylex"),
                "Error message should mention 'polylex'"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `handles mix of successes and failures`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-mixed-test").toFile()
        try {
            val files = listOf(
                createTempSmlFile(tempDir, "a.sml", "val x = 1"),
                createTempSmlFile(tempDir, "b.sml", "val y = 2"),
                createTempSmlFile(tempDir, "c.sml", "val z = 3"),
            )
            val tokens = "VAL ID(x) INT(1)"
            // Oracle fails for the 3rd file (based on content marker)
            val oracle: (String) -> LexerResult = { src ->
                if (src.contains("z")) LexerResult.Failure("oracle error on z") else LexerResult.Success(tokens, 0)
            }
            val polylex: suspend (String) -> LexerResult = { LexerResult.Success(tokens, 0) }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(3, results.size, "Should return one result per file")
            val successes = results.filterIsInstance<BatchFileResult.Success>()
            val failures = results.filterIsInstance<BatchFileResult.Failure>()
            assertEquals(2, successes.size, "Should have 2 successes")
            assertEquals(1, failures.size, "Should have 1 failure")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `processes files concurrently`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-concurrency-test").toFile()
        try {
            val fileCount = 5
            val delayMs = 100L
            val files = (1..fileCount).map { createTempSmlFile(tempDir, "file$it.sml", "val x$it = $it") }
            val counter = AtomicInteger(0)
            val oracle: (String) -> LexerResult = { LexerResult.Success("VAL", 0) }
            val polylex: suspend (String) -> LexerResult = {
                counter.incrementAndGet()
                delay(delayMs)
                LexerResult.Success("VAL", 0)
            }

            val startMs = System.currentTimeMillis()
            val results = BatchOrchestrator.processAll(files, oracle, polylex)
            val elapsed = System.currentTimeMillis() - startMs

            assertEquals(fileCount, results.size, "Should process all files")
            assertEquals(fileCount, counter.get(), "All polylex lambdas should have been called")
            // If sequential: elapsed >= fileCount * delayMs. If concurrent: elapsed << fileCount * delayMs.
            assertTrue(
                elapsed < fileCount * delayMs,
                "Should be concurrent: elapsed=${elapsed}ms but sequential would be ${fileCount * delayMs}ms"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `file containing comment syntax returns CommentSkipped`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-comment-test").toFile()
        try {
            val files = listOf(createTempSmlFile(tempDir, "a.sml", "(* this is a comment *) val x = 1"))
            val oracleCalled = AtomicInteger(0)
            val polylexCalled = AtomicInteger(0)
            val oracle: (String) -> LexerResult = { oracleCalled.incrementAndGet(); LexerResult.Success("VAL", 0) }
            val polylex: suspend (String) -> LexerResult = { polylexCalled.incrementAndGet(); LexerResult.Success("VAL", 0) }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(1, results.size)
            assertIs<BatchFileResult.Success>(results[0])
            assertIs<ComparisonResult.CommentSkipped>((results[0] as BatchFileResult.Success).comparisonResult)
            assertEquals(0, oracleCalled.get(), "Oracle should not be invoked for comment-containing files")
            assertEquals(0, polylexCalled.get(), "Polylex should not be invoked for comment-containing files")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `file without comment syntax compares normally`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-no-comment-test").toFile()
        try {
            val files = listOf(createTempSmlFile(tempDir, "a.sml", "val x = 1"))
            val oracle: (String) -> LexerResult = { LexerResult.Success("VAL ID(x) INT(1)", 0) }
            val polylex: suspend (String) -> LexerResult = { LexerResult.Success("VAL ID(x) INT(1)", 0) }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(1, results.size)
            assertIs<BatchFileResult.Success>(results[0])
            assertIs<ComparisonResult.Match>((results[0] as BatchFileResult.Success).comparisonResult)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `error count mismatch produces ErrorMismatch result`() = runBlocking {
        val tempDir = createTempDirectory("orchestrator-error-mismatch-test").toFile()
        try {
            val files = listOf(createTempSmlFile(tempDir, "a.sml", "val x = 1"))
            val oracle: (String) -> LexerResult = { LexerResult.Success("VAL ID(x) INT(1)", 2) }
            val polylex: suspend (String) -> LexerResult = { LexerResult.Success("VAL ID(x) INT(1)", 0) }

            val results = BatchOrchestrator.processAll(files, oracle, polylex)

            assertEquals(1, results.size)
            assertIs<BatchFileResult.Success>(results[0])
            val cr = (results[0] as BatchFileResult.Success).comparisonResult
            assertIs<ComparisonResult.ErrorMismatch>(cr)
            assertEquals(2, cr.oracleErrors)
            assertEquals(0, cr.polylexErrors)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
