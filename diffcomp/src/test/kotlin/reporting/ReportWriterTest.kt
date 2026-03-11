package reporting

import batch.BatchFileResult
import comparison.ComparisonResult
import comparison.Mismatch
import comparison.MismatchType
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportWriterTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `writeAll produces one JSON file per result`() {
        val outputDir = createTempDirectory("report-test").toFile()
        val results = listOf(
            BatchFileResult.Success(File("match.sml"), ComparisonResult.Match),
            BatchFileResult.Success(
                File("diff.sml"),
                ComparisonResult.Diff(
                    listOf(
                        Mismatch(MismatchType.ORACLE_ONLY, 0, -1, "tok", null)
                    )
                )
            ),
            BatchFileResult.Failure(File("fail.sml"), "some error"),
        )

        ReportWriter.writeAll(results, outputDir)

        val files = outputDir.listFiles()?.map { it.name } ?: emptyList()
        assertEquals(3, files.size, "Expected 3 JSON files")
        assertTrue(files.contains("match.json"))
        assertTrue(files.contains("diff.json"))
        assertTrue(files.contains("fail.json"))
    }

    @Test
    fun `match result produces status MATCH with empty mismatches`() {
        val outputDir = createTempDirectory("report-test").toFile()
        val result = BatchFileResult.Success(File("test.sml"), ComparisonResult.Match)

        ReportWriter.writeAll(listOf(result), outputDir)

        val reportFile = File(outputDir, "test.json")
        val report = json.decodeFromString<FileReport>(reportFile.readText())
        assertEquals(Status.MATCH, report.status)
        assertEquals(0, report.mismatchCount)
        assertTrue(report.mismatches.isEmpty())
        assertNull(report.error)
    }

    @Test
    fun `diff result produces status DIFF with populated mismatches`() {
        val outputDir = createTempDirectory("report-test").toFile()
        val mismatches = listOf(
            Mismatch(MismatchType.ORACLE_ONLY, 0, -1, "oracleTok", null),
            Mismatch(MismatchType.TOKEN_TYPE_MISMATCH, 2, 3, "oracleTok2", "polylexTok2"),
        )
        val result = BatchFileResult.Success(File("diff.sml"), ComparisonResult.Diff(mismatches))

        ReportWriter.writeAll(listOf(result), outputDir)

        val reportFile = File(outputDir, "diff.json")
        val report = json.decodeFromString<FileReport>(reportFile.readText())
        assertEquals(Status.DIFF, report.status)
        assertEquals(2, report.mismatchCount)
        assertEquals(2, report.mismatches.size)
        assertEquals(MismatchType.ORACLE_ONLY, report.mismatches[0].type)
        assertEquals(0, report.mismatches[0].oracleIndex)
        assertEquals(-1, report.mismatches[0].polylexIndex)
        assertEquals("oracleTok", report.mismatches[0].oracleToken)
        assertNull(report.mismatches[0].polylexToken)
        assertEquals(MismatchType.TOKEN_TYPE_MISMATCH, report.mismatches[1].type)
        assertEquals(2, report.mismatches[1].oracleIndex)
        assertEquals(3, report.mismatches[1].polylexIndex)
        assertEquals("oracleTok2", report.mismatches[1].oracleToken)
        assertEquals("polylexTok2", report.mismatches[1].polylexToken)
        assertNull(report.error)
    }

    @Test
    fun `failure result produces status FAILURE with error field`() {
        val outputDir = createTempDirectory("report-test").toFile()
        val result = BatchFileResult.Failure(File("fail.sml"), "some error")

        ReportWriter.writeAll(listOf(result), outputDir)

        val reportFile = File(outputDir, "fail.json")
        val report = json.decodeFromString<FileReport>(reportFile.readText())
        assertEquals(Status.FAILURE, report.status)
        assertEquals("some error", report.error)
        assertEquals(0, report.mismatchCount)
        assertTrue(report.mismatches.isEmpty())
    }

    @Test
    fun `output file named after input file with json extension`() {
        val outputDir = createTempDirectory("report-test").toFile()
        val result = BatchFileResult.Success(File("test.sml"), ComparisonResult.Match)

        ReportWriter.writeAll(listOf(result), outputDir)

        val reportFile = File(outputDir, "test.json")
        assertTrue(reportFile.exists(), "Expected test.json to be created")
    }

    @Test
    fun `output directory is created if it does not exist`() {
        val baseDir = createTempDirectory("report-test").toFile()
        val nonExistentDir = File(baseDir, "subdir/nested")
        val result = BatchFileResult.Success(File("test.sml"), ComparisonResult.Match)

        ReportWriter.writeAll(listOf(result), nonExistentDir)

        assertTrue(nonExistentDir.exists(), "Expected output directory to be created")
        assertTrue(File(nonExistentDir, "test.json").exists(), "Expected JSON file to be created in new directory")
    }

    @Test
    fun `comment skipped result produces status COMMENT_SKIPPED`() {
        val outputDir = createTempDirectory("report-test").toFile()
        val result = BatchFileResult.Success(File("commented.sml"), ComparisonResult.CommentSkipped)

        ReportWriter.writeAll(listOf(result), outputDir)

        val reportFile = File(outputDir, "commented.json")
        val report = json.decodeFromString<FileReport>(reportFile.readText())
        assertEquals(Status.COMMENT_SKIPPED, report.status)
        assertEquals(0, report.mismatchCount)
        assertTrue(report.mismatches.isEmpty())
        assertNull(report.error)
    }

    @Test
    fun `writeAll with empty results list produces no files`() {
        val outputDir = createTempDirectory("report-empty-test").toFile()
        ReportWriter.writeAll(emptyList(), outputDir)

        val files = outputDir.listFiles() ?: emptyArray()
        assertEquals(0, files.size, "Expected no JSON files for empty results")
    }
}
