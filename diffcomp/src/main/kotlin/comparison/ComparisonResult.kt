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
}
