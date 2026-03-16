package reporting

import batch.BatchFileResult
import kotlinx.serialization.json.Json
import java.io.File

private val prettyJson = Json { prettyPrint = true }

object ReportWriter {
    fun writeAll(results: List<BatchFileResult>, outputDir: File) {
        outputDir.mkdirs()
        for (result in results) {
            val report = result.toFileReport()
            if (report.status == Status.MATCH) continue
            val json = prettyJson.encodeToString(report)
            val outFile = File(outputDir, "${report.status.name.uppercase()}>>=${result.file.nameWithoutExtension}.json")
            outFile.writeText(json)
        }
    }
}
