package batch

import comparison.ComparisonResult
import java.io.File

/**
 * Per-file outcome of batch processing.
 * Either a successful comparison or a captured lexer failure. Both variants carry the originating [file] for reporting.
 */
sealed class BatchFileResult {
    abstract val file: File

    data class Success(override val file: File, val comparisonResult: ComparisonResult) : BatchFileResult()
    data class Failure(override val file: File, val error: String) : BatchFileResult()
}
