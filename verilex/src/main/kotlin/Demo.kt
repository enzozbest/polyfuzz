import tokenizer.SMLTokeniser

fun main() {
    val input = """
    
  val  z:: _= 1
exception  u of ly  kiy and  k9 = fT exception bH8
;h        
    """.trimIndent()
    // Tokenise using SMLTokeniser and get tokens without trivia
    val verilexTokens = SMLTokeniser.tokenise(input).withoutTrivia()
    val verilexOutput = verilexTokens.toCompactString()

    // Call polylex to get its result
    val polylexOutput = runPolylex(input)

    // Compare and report
    println("Input: \"$input\"")
    println()
    println("Verilex output: $verilexOutput")
    println("Polylex output: $polylexOutput")
    println()

    if (verilexOutput == polylexOutput) {
        println("✓ MATCH: Both tokenisers produced identical output")
    } else {
        println("✗ DIFFERENT: The tokenisers produced different output")
        println()
        println("Differences:")
        showDifferences(verilexOutput, polylexOutput)
    }
}

/**
 * Run the polylex program on the given input and return its output.
 */
fun runPolylex(input: String): String {
    val process = ProcessBuilder("../polylex-harness/polylex")
        .redirectErrorStream(true)
        .start()

    // Write input to polylex's stdin
    process.outputStream.bufferedWriter().use { writer ->
        writer.write(input)
    }

    // Read output from polylex's stdout
    val output = process.inputStream.bufferedReader().use { reader ->
        reader.readText()
    }

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        error("polylex exited with code $exitCode: $output")
    }

    return output.trim()
}

/**
 * Show a simple diff between two outputs.
 */
fun showDifferences(verilex: String, polylex: String) {
    val verilexTokens = verilex.split(" ")
    val polylexTokens = polylex.split(" ")

    val maxLen = maxOf(verilexTokens.size, polylexTokens.size)

    for (i in 0 until maxLen) {
        val v = verilexTokens.getOrNull(i) ?: "<missing>"
        val p = polylexTokens.getOrNull(i) ?: "<missing>"
        val marker = if (v == p) " " else "!"
        println("  $marker [$i] Verilex: $v | Polylex: $p")
    }
}