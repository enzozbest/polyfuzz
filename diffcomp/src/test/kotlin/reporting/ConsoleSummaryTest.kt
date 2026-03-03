package reporting

import batch.BatchFileResult
import comparison.ComparisonResult
import comparison.Mismatch
import comparison.MismatchType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleSummaryTest {

    @Test
    fun `prints aggregate summary with correct counts`() {
        val results = listOf(
            BatchFileResult.Success(File("a.sml"), ComparisonResult.Match),
            BatchFileResult.Success(File("b.sml"), ComparisonResult.Match),
            BatchFileResult.Success(
                File("c.sml"),
                ComparisonResult.Diff(listOf(Mismatch(MismatchType.ORACLE_ONLY, 0, -1, "t", null)))
            ),
            BatchFileResult.Success(
                File("d.sml"),
                ComparisonResult.Diff(listOf(Mismatch(MismatchType.POLYLEX_ONLY, -1, 0, null, "t")))
            ),
            BatchFileResult.Failure(File("e.sml"), "error"),
        )
        val output = mutableListOf<String>()

        ConsoleSummary.print(results, output::add)

        val joined = output.joinToString("\n")
        assertTrue(joined.contains("Total files processed : 5"), "Expected total files = 5, got:\n$joined")
        assertTrue(joined.contains("Matching            : 2"), "Expected matches = 2, got:\n$joined")
        assertTrue(joined.contains("Mismatching         : 2"), "Expected mismatches = 2, got:\n$joined")
        assertTrue(joined.contains("Failures            : 1"), "Expected failures = 1, got:\n$joined")
    }

    @Test
    fun `prints total mismatch count across all diff files`() {
        val results = listOf(
            BatchFileResult.Success(
                File("a.sml"),
                ComparisonResult.Diff(
                    listOf(
                        Mismatch(MismatchType.ORACLE_ONLY, 0, -1, "t", null),
                        Mismatch(MismatchType.POLYLEX_ONLY, -1, 0, null, "t"),
                        Mismatch(MismatchType.TOKEN_TYPE_MISMATCH, 1, 1, "t1", "t2"),
                    )
                )
            ),
            BatchFileResult.Success(
                File("b.sml"),
                ComparisonResult.Diff(
                    listOf(
                        Mismatch(MismatchType.TOKEN_TEXT_MISMATCH, 2, 2, "tok", "tok2"),
                        Mismatch(MismatchType.WRONG_TOKEN_POSITION, 3, 5, "tok3", "tok3"),
                    )
                )
            ),
        )
        val output = mutableListOf<String>()

        ConsoleSummary.print(results, output::add)

        val joined = output.joinToString("\n")
        assertTrue(joined.contains("Total mismatches    : 5"), "Expected total mismatches = 5, got:\n$joined")
    }

    @Test
    fun `prints per-file mismatch details for diff files`() {
        val results = listOf(
            BatchFileResult.Success(
                File("mismatch.sml"),
                ComparisonResult.Diff(
                    listOf(
                        Mismatch(MismatchType.ORACLE_ONLY, 0, -1, "tok", null),
                        Mismatch(MismatchType.TOKEN_TYPE_MISMATCH, 1, 1, "tok1", "tok2"),
                    )
                )
            ),
        )
        val output = mutableListOf<String>()

        ConsoleSummary.print(results, output::add)

        val joined = output.joinToString("\n")
        assertTrue(joined.contains("mismatch.sml"), "Expected file name in output, got:\n$joined")
        assertTrue(joined.contains("ORACLE_ONLY"), "Expected ORACLE_ONLY category in output, got:\n$joined")
        assertTrue(joined.contains("TOKEN_TYPE_MISMATCH"), "Expected TOKEN_TYPE_MISMATCH category in output, got:\n$joined")
        assertTrue(joined.contains("2 mismatch"), "Expected mismatch count in output, got:\n$joined")
    }

    @Test
    fun `does not list matching files in per-file details`() {
        val results = listOf(
            BatchFileResult.Success(File("match.sml"), ComparisonResult.Match),
        )
        val output = mutableListOf<String>()

        ConsoleSummary.print(results, output::add)

        val joined = output.joinToString("\n")
        assertFalse(joined.contains("match.sml"), "Match file should not appear in per-file details, got:\n$joined")
    }

    @Test
    fun `prints failure details`() {
        val results = listOf(
            BatchFileResult.Failure(File("crashed.sml"), "lexer crashed"),
        )
        val output = mutableListOf<String>()

        ConsoleSummary.print(results, output::add)

        val joined = output.joinToString("\n")
        assertTrue(joined.contains("crashed.sml"), "Expected failure file name in output, got:\n$joined")
        assertTrue(joined.contains("lexer crashed"), "Expected error message in output, got:\n$joined")
    }

    @Test
    fun `prints summary with all zeros for empty results`() {
        val output = mutableListOf<String>()

        ConsoleSummary.print(emptyList(), output::add)

        val joined = output.joinToString("\n")
        assertTrue(joined.contains("Total files processed : 0"), "Expected 0 total files, got:\n$joined")
        assertTrue(joined.contains("Matching            : 0"), "Expected 0 matches, got:\n$joined")
        assertTrue(joined.contains("Mismatching         : 0"), "Expected 0 mismatches, got:\n$joined")
        assertTrue(joined.contains("Failures            : 0"), "Expected 0 failures, got:\n$joined")
        assertTrue(joined.contains("Total mismatches    : 0"), "Expected 0 total mismatches, got:\n$joined")
        assertFalse(joined.contains("Per-File"), "Should not print per-file details for empty results")
        assertFalse(joined.contains("=== Failures ==="), "Should not print failures section for empty results")
    }
}
