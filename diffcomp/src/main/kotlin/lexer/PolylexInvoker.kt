package lexer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Invokes the polylex_fuzz external binary via ProcessBuilder. Writes SML source to stdin, reads token stream from stdout.
 *
 * IMPORTANT: Uses redirectErrorStream(true) to merge stderr into stdout, preventing deadlock when pipe buffers fill.
 * Reads all output before calling waitFor().
 */
object PolylexInvoker {
    /**
     * Invoke polylex_fuzz with the given SML source.
     *
     * @param binaryPath Absolute or relative path to polylex_fuzz binary
     * @param smlSource The SML source code to tokenise
     * @return LexerResult.Success with token stream, or LexerResult.Failure with error
     */
    suspend fun invoke(binaryPath: String, smlSource: String): LexerResult =
        withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(binaryPath)
                    .redirectErrorStream(true)
                    .start()

                process.outputStream.bufferedWriter().use { it.write(smlSource) }

                // Read all output (stdout + merged stderr) BEFORE waitFor to avoid deadlock
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    LexerResult.Failure("polylex_fuzz exited with code $exitCode: $output")
                } else if (output.isBlank()) {
                    LexerResult.Failure("polylex_fuzz produced empty output")
                } else {
                    LexerResult.Success(output.trimEnd().lines().joinToString(" "))
                }
            } catch (e: Exception) {
                LexerResult.Failure("polylex_fuzz invocation failed: ${e.message}")
            }
        }
}
