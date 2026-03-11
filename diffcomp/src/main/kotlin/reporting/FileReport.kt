package reporting

import batch.BatchFileResult
import comparison.ComparisonResult
import comparison.MismatchType
import kotlinx.serialization.Serializable

enum class Status { MATCH, DIFF, ERROR_MISMATCH, COMMENT_SKIPPED, FAILURE }

@Serializable
data class MismatchReport(
    val type: MismatchType,
    val oracleIndex: Int,
    val polylexIndex: Int,
    val oracleToken: String?,
    val polylexToken: String?,
)

@Serializable
data class FileReport(
    val filePath: String,
    val status: Status,
    val mismatchCount: Int,
    val mismatches: List<MismatchReport>,
    val error: String?,
    val oracleErrors: Int? = null,
    val polylexErrors: Int? = null,
)

fun BatchFileResult.toFileReport(): FileReport = when (this) {
    is BatchFileResult.Success -> when (val cr = comparisonResult) {
        is ComparisonResult.Match -> FileReport(
            filePath = file.absoluteFile.invariantSeparatorsPath,
            status = Status.MATCH,
            mismatchCount = 0,
            mismatches = emptyList(),
            error = null,
        )
        is ComparisonResult.Diff -> FileReport(
            filePath = file.absoluteFile.invariantSeparatorsPath,
            status = Status.DIFF,
            mismatchCount = cr.mismatchCount,
            mismatches = cr.mismatches.map { m ->
                MismatchReport(
                    type = m.type,
                    oracleIndex = m.oracleIndex,
                    polylexIndex = m.polylexIndex,
                    oracleToken = m.oracleToken,
                    polylexToken = m.polylexToken,
                )
            },
            error = null,
        )
        is ComparisonResult.ErrorMismatch -> FileReport(
            filePath = file.absoluteFile.invariantSeparatorsPath,
            status = Status.ERROR_MISMATCH,
            mismatchCount = 0,
            mismatches = emptyList(),
            error = null,
            oracleErrors = cr.oracleErrors,
            polylexErrors = cr.polylexErrors,
        )
        is ComparisonResult.CommentSkipped -> FileReport(
            filePath = file.absoluteFile.invariantSeparatorsPath,
            status = Status.COMMENT_SKIPPED,
            mismatchCount = 0,
            mismatches = emptyList(),
            error = null,
        )
    }
    is BatchFileResult.Failure -> FileReport(
        filePath = file.absoluteFile.invariantSeparatorsPath,
        status = Status.FAILURE,
        mismatchCount = 0,
        mismatches = emptyList(),
        error = error,
    )
}
