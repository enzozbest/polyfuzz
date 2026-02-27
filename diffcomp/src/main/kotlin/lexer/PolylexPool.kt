package lexer

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Semaphore-bounded pool for polylex process invocations.
 * Limits the number of concurrent polylex_fuzz processes to [poolSize]. Callers suspend when all permits are taken.
 *
 * @param binaryPath Path to the polylex_fuzz binary
 * @param poolSize Maximum concurrent polylex processes (defaults to available processors)
 */
class PolylexPool(
    private val binaryPath: String,
    poolSize: Int = Runtime.getRuntime().availableProcessors()
) {
    private val semaphore = Semaphore(permits = poolSize)

    /**
     * Tokenise SML source through polylex, bounded by the pool semaphore. Suspends if all pool slots are occupied.
     */
    suspend fun tokenise(smlSource: String): LexerResult =
        semaphore.withPermit {
            PolylexInvoker.invoke(binaryPath, smlSource)
        }
}
