package batch

import comparison.ComparisonEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import lexer.LexerResult
import java.io.File

/**
 * Fan-out batch processor that runs all files through both lexers concurrently.
 *
 * Accepts functional parameters for both tokenisers to allow test injection of fakes without changing the main API.
 */
object BatchOrchestrator {
    /**
     * Processes all [files] concurrently through both lexers and the comparison engine.
     *
     * @param files List of .sml files to process
     * @param oracleTokenise Synchronous oracle (verilex) tokeniser function
     * @param polylexTokenise Suspending polylex tokeniser function (bounds concurrency via PolylexPool semaphore)
     * @return One [BatchFileResult] per input file; per-file failures are captured, not thrown
     */
    suspend fun processAll(
        files: List<File>,
        oracleTokenise: (String) -> LexerResult,
        polylexTokenise: suspend (String) -> LexerResult,
    ): List<BatchFileResult> = coroutineScope {
        files.map { file ->
            async {
                processFile(file, oracleTokenise, polylexTokenise)
            }
        }.awaitAll()
    }

    private suspend fun processFile(
        file: File,
        oracleTokenise: (String) -> LexerResult,
        polylexTokenise: suspend (String) -> LexerResult,
    ): BatchFileResult {
        val source = file.readText()
        val oracleResult = oracleTokenise(source)
        val polylexResult = polylexTokenise(source)

        return when {
            oracleResult is LexerResult.Failure ->
                BatchFileResult.Failure(file, "oracle: ${oracleResult.error}")
            polylexResult is LexerResult.Failure ->
                BatchFileResult.Failure(file, "polylex: ${polylexResult.error}")
            oracleResult is LexerResult.Success && polylexResult is LexerResult.Success ->
                BatchFileResult.Success(
                    file,
                    ComparisonEngine.compare(oracleResult.tokenStream, polylexResult.tokenStream)
                )
            else ->
                BatchFileResult.Failure(file, "unexpected result combination") //Logically unreachable due to sealed class, but required for exhaustiveness
        }
    }
}
