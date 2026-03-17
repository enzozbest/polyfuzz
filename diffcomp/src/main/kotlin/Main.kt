import batch.BatchOrchestrator
import batch.FileDiscovery
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import lexer.PolylexPool
import lexer.VerilexInvoker
import java.nio.file.Path
import reporting.ConsoleSummary
import reporting.ReportWriter

class DiffCompCommand : CliktCommand(name = "diffcomp") {
    val inputDir: Path by argument("INPUT_DIR")
        .path(mustExist = true, canBeFile = false, mustBeReadable = true)
        .help("Directory of .sml files to compare")

    val parallelism: Int by option("--parallelism", "-p")
        .int()
        .default(Runtime.getRuntime().availableProcessors())
        .help("Maximum concurrent polylex processes (default: available processors)")

    val polylexBinary: String by option("--polylex", "-l")
        .default("../polylex-harness-fixed/polylex_fuzz")
        .help("Path to polylex_fuzz binary")

    val outputDir: Path by option("--output-dir", "-o")
        .path(canBeFile = false)
        .required()
        .help("Directory to write per-file JSON reports")

    override fun run() {
        echo("DiffComp — Differential Comparator")
        echo("===================================")
        echo()

        val files = FileDiscovery.findSmlFiles(inputDir.toFile())
        if (files.isEmpty()) {
            echo("No .sml files found in $inputDir")
            return
        }
        echo("Found ${files.size} .sml file(s) in $inputDir")
        echo("Parallelism: $parallelism")
        echo()

        val pool = PolylexPool(binaryPath = polylexBinary, poolSize = parallelism)
        val results = runBlocking {
            BatchOrchestrator.processAll(
                files = files,
                oracleTokenise = VerilexInvoker::invoke,
                polylexTokenise = pool::tokenise,
            )
        }

        //Write JSON reports
        ReportWriter.writeAll(results, outputDir.toFile())
        echo("JSON reports written to: ${outputDir.toFile().canonicalPath}")
        echo()

        ConsoleSummary.print(results) { echo(it) }
    }
}

fun main(args: Array<String>) = DiffCompCommand().main(args)
