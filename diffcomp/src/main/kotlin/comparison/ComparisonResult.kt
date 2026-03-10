package comparison

sealed class ComparisonResult {
    /** Both token streams hash-equal — no diff needed. */
    object Match : ComparisonResult()

    /**
     * Hashes differ. [mismatches] is the full categorised diff.
     *
     * Oracle (verilex) is always the reference; every entry is attributed to polylex.
     */
    data class Diff(val mismatches: List<Mismatch>) : ComparisonResult() {
        val mismatchCount: Int get() = mismatches.size
    }

    /** The two lexers disagree on how many error tokens the input produced. */
    data class ErrorMismatch(val oracleErrors: Int, val polylexErrors: Int) : ComparisonResult()

    /** Input contains SML comments which the oracle cannot handle; skipped for manual review. */
    object CommentSkipped : ComparisonResult()
}
