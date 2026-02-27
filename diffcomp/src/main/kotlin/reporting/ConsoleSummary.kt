package reporting

import batch.BatchFileResult
import comparison.ComparisonResult

object ConsoleSummary {
    fun print(results: List<BatchFileResult>, echoFn: (String) -> Unit) {
        val successes = results.filterIsInstance<BatchFileResult.Success>()
        val failures = results.filterIsInstance<BatchFileResult.Failure>()
        val matches = successes.count { it.comparisonResult is ComparisonResult.Match }
        val diffs = successes.filter { it.comparisonResult is ComparisonResult.Diff }
        val totalMismatches = diffs.sumOf { (it.comparisonResult as ComparisonResult.Diff).mismatchCount }

        echoFn("=== DiffComp Summary ===")
        echoFn("Total files processed : ${results.size}")
        echoFn("  Matching            : $matches")
        echoFn("  Mismatching         : ${diffs.size}")
        echoFn("  Failures            : ${failures.size}")
        echoFn("  Total mismatches    : $totalMismatches")

        if (diffs.isNotEmpty()) {
            echoFn("")
            echoFn("=== Per-File Mismatch Details ===")
            for (s in diffs) {
                val diff = s.comparisonResult as ComparisonResult.Diff
                val categories = diff.mismatches.groupingBy { it.type }.eachCount()
                echoFn("  ${s.file.name}: ${diff.mismatchCount} mismatch(es)")
                for ((type, count) in categories) {
                    echoFn("    - $type: $count")
                }
            }
        }

        if (failures.isNotEmpty()) {
            echoFn("")
            echoFn("=== Failures ===")
            for (f in failures) {
                echoFn("  ${f.file.name}: ${f.error}")
            }
        }
    }
}
