package reporting

import batch.BatchFileResult
import comparison.ComparisonResult
import kotlinx.serialization.Serializable

@Serializable
data class MismatchReport(
    val type: String,
    val oracleIndex: Int,
    val polylexIndex: Int,
    val oracleToken: String?,
    val polylexToken: String?,
)

@Serializable
data class FileReport(
    val filePath: String,
    val status: String,         // "MATCH", "DIFF", "ERROR_MISMATCH", "COMMENT_SKIPPED", "FAILURE"
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
            status = "MATCH",
            mismatchCount = 0,
            mismatches = emptyList(),
            error = null,
        )
        is ComparisonResult.Diff -> FileReport(
            filePath = file.absoluteFile.invariantSeparatorsPath,
            status = "DIFF",
            mismatchCount = cr.mismatchCount,
            mismatches = cr.mismatches.map { m ->
                MismatchReport(
                    type = m.type.name,
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
            status = "ERROR_MISMATCH",
            mismatchCount = 0,
            mismatches = emptyList(),
            error = null,
            oracleErrors = cr.oracleErrors,
            polylexErrors = cr.polylexErrors,
        )
        is ComparisonResult.CommentSkipped -> FileReport(
            filePath = file.absoluteFile.invariantSeparatorsPath,
            status = "COMMENT_SKIPPED",
            mismatchCount = 0,
            mismatches = emptyList(),
            error = null,
        )
    }
    is BatchFileResult.Failure -> FileReport(
        filePath = file.absoluteFile.invariantSeparatorsPath,
        status = "FAILURE",
        mismatchCount = 0,
        mismatches = emptyList(),
        error = error,
    )
}
