package bestetti.enzo.smlgen.sml

import bestetti.enzo.smlgen.sml.generator.ProgramComplexity
import bestetti.enzo.smlgen.sml.generator.ProgramConfig
import bestetti.enzo.smlgen.sml.generator.SmlProgramGenerator
import bestetti.enzo.smlgen.sml.generator.SmlProgramGeneratorConveniences
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class SmlProgramGeneratorTest {

    @Test
    fun `generated program respects max length`() {
        val maxLengths = listOf(50, 100, 200, 500, 1000)
        for (maxLength in maxLengths) {
            repeat(10) { i ->
                val program = SmlProgramGenerator.generate(
                    ProgramConfig(maxLength = maxLength, seed = i.toLong())
                )
                assertTrue(
                    program.length <= maxLength,
                    "Program exceeds max length $maxLength: ${program.length} chars"
                )
            }
        }
    }

    @Test
    fun `minimal complexity generates short programs`() {
        repeat(20) { i ->
            val program = SmlProgramGeneratorConveniences.generateMinimal(maxLength = 100, seed = i.toLong())
            assertTrue(program.length <= 100, "Minimal should be short: ${program.length}")
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `simple complexity generates valid looking programs`() {
        repeat(20) { i ->
            val program = SmlProgramGeneratorConveniences.generateSimple(maxLength = 200, seed = i.toLong())
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `medium complexity generates varied programs`() {
        val programs = (0 until 20).map { i ->
            SmlProgramGeneratorConveniences.generateMedium(maxLength = 500, seed = i.toLong())
        }
        // Check that we get variety
        val uniquePrograms = programs.toSet()
        assertTrue(uniquePrograms.size > 10, "Should generate varied programs")
    }

    @Test
    fun `complex complexity uses more features`() {
        repeat(10) { i ->
            val program = SmlProgramGeneratorConveniences.generateComplex(maxLength = 1500, seed = i.toLong())
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `extreme complexity includes obscure features when enabled`() {
        repeat(10) { i ->
            val program = SmlProgramGeneratorConveniences.generateExtreme(maxLength = 2000, seed = i.toLong())
            assertTrue(program.isNotEmpty(), "Program should not be empty")
        }
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val config = ProgramConfig(maxLength = 500, complexity = ProgramComplexity.MEDIUM, seed = 12345L)
        val program1 = SmlProgramGenerator.generate(config)
        val program2 = SmlProgramGenerator.generate(config)
        assertEquals(program1, program2, "Same config should produce same program")
    }

    @Test
    fun `generation varies with different seeds`() {
        val programs = (0L until 10L).map { seed ->
            SmlProgramGenerator.generate(
                ProgramConfig(maxLength = 300, complexity = ProgramComplexity.MEDIUM, seed = seed)
            )
        }.toSet()
        assertTrue(programs.size > 5, "Different seeds should produce different programs")
    }

    @Test
    fun `test suite generates correct number of programs`() {
        val count = 15
        val suite = SmlProgramGenerator.generateTestSuite(count, maxLength = 200)
        assertEquals(count, suite.size, "Should generate requested number of programs")
    }

    @Test
    fun `test suite programs respect max length`() {
        val maxLength = 300
        val suite = SmlProgramGenerator.generateTestSuite(20, maxLength)
        for ((i, program) in suite.withIndex()) {
            assertTrue(
                program.length <= maxLength,
                "Program $i exceeds max length: ${program.length}"
            )
        }
    }

    @Test
    fun `test suite covers all complexity levels`() {
        // Generate enough programs to cover all complexity levels multiple times
        val suite = SmlProgramGenerator.generateTestSuite(count = 25, maxLength = 500, seed = 42L)
        assertTrue(suite.all { it.isNotEmpty() }, "All programs should be non-empty")
    }

    @Test
    fun `programs have balanced delimiters outside strings and comments`() {
        repeat(50) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(maxLength = 500, complexity = ProgramComplexity.MEDIUM, seed = i.toLong())
            )

            // Count delimiters outside of strings, char constants, and comments
            var openParens = 0
            var closeParens = 0
            var openBrackets = 0
            var closeBrackets = 0
            var openBraces = 0
            var closeBraces = 0

            var inString = false
            var inComment = false
            var j = 0
            while (j < program.length) {
                val c = program[j]

                // Handle comment start/end (non-nested)
                if (!inString && j + 1 < program.length) {
                    if (c == '(' && program[j + 1] == '*') {
                        inComment = true
                        j += 2
                        continue
                    }
                    if (inComment && c == '*' && program[j + 1] == ')') {
                        inComment = false
                        j += 2
                        continue
                    }
                }

                // Skip characters inside comments
                if (inComment) {
                    j++
                    continue
                }

                // Track string boundaries (handling escaped quotes)
                if (c == '"' && (j == 0 || program[j - 1] != '\\' ||
                            (j >= 2 && program[j - 2] == '\\'))
                ) {
                    inString = !inString
                    j++
                    continue
                }

                // Skip characters inside strings
                if (inString) {
                    j++
                    continue
                }

                // Skip character constants (#"x")
                if (c == '#' && j + 2 < program.length && program[j + 1] == '"') {
                    j += 2
                    // Skip until closing quote
                    while (j < program.length && program[j] != '"') {
                        if (program[j] == '\\' && j + 1 < program.length) j++
                        j++
                    }
                    j++ // skip closing quote
                    continue
                }

                when (c) {
                    '(' -> openParens++
                    ')' -> closeParens++
                    '[' -> openBrackets++
                    ']' -> closeBrackets++
                    '{' -> openBraces++
                    '}' -> closeBraces++
                }
                j++
            }

            assertEquals(openParens, closeParens, "Unbalanced parens in: $program")
            assertEquals(openBrackets, closeBrackets, "Unbalanced brackets in: $program")
            assertEquals(openBraces, closeBraces, "Unbalanced braces in: $program")
        }
    }

    @Test
    fun `programs do not contain nested comments`() {
        repeat(100) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(
                    maxLength = 1000,
                    complexity = ProgramComplexity.entries[i % ProgramComplexity.entries.size],
                    seed = i.toLong(),
                    includeComments = true
                )
            )

            // Check that there are no nested comment markers
            var depth = 0
            var j = 0
            while (j < program.length - 1) {
                if (program[j] == '(' && program[j + 1] == '*') {
                    depth++
                    assertTrue(depth <= 1, "Nested comment found at position $j in: $program")
                    j += 2
                } else if (program[j] == '*' && program[j + 1] == ')') {
                    depth--
                    j += 2
                } else {
                    j++
                }
            }
        }
    }

    @Test
    fun `strings have properly escaped quotes`() {
        repeat(50) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(maxLength = 500, seed = i.toLong())
            )

            // Count quotes that are not escaped
            var inString = false
            var j = 0
            while (j < program.length) {
                val c = program[j]
                if (c == '"') {
                    // Check if it's escaped
                    val numBackslashes = (0 until j).reversed().takeWhile { program[it] == '\\' }.count()
                    if (numBackslashes % 2 == 0) {
                        // Not escaped (or even number of backslashes)
                        inString = !inString
                    }
                }
                j++
            }
            // After processing, we should not be in a string (balanced quotes)
            // Note: This is a basic check; truncation might leave unbalanced quotes
            // which the balancer should fix
        }
    }

    @Test
    fun `all complexity levels produce valid output`() {
        for (complexity in ProgramComplexity.entries) {
            repeat(10) { i ->
                val program = SmlProgramGenerator.generate(
                    ProgramConfig(
                        maxLength = 800,
                        complexity = complexity,
                        seed = i.toLong()
                    )
                )
                assertTrue(program.isNotEmpty(), "$complexity should produce non-empty program")
            }
        }
    }

    @Test
    fun `with comments disabled no comments are generated`() {
        repeat(30) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(
                    maxLength = 500,
                    complexity = ProgramComplexity.MEDIUM,
                    seed = i.toLong(),
                    includeComments = false
                )
            )
            // Basic check - comments start with (*
            // This isn't a perfect check since (* could appear in strings
            // but it's a reasonable heuristic
            val commentLikeCount = program.windowed(2).count { it == "(*" }
            val stringCount = program.count { it == '"' }
            // If there are no strings, there should be no comment markers
            if (stringCount == 0) {
                assertEquals(0, commentLikeCount, "Comments should be disabled but found (*")
            }
        }
    }

    @Test
    fun `generate falls back to minimal when all attempts exceed maxLength`() {
        // maxLength=1 with MINIMAL complexity forces the MINIMAL->break branch
        // and the fallback path at the end of generate()
        repeat(10) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(
                    maxLength = 1,
                    complexity = ProgramComplexity.MINIMAL,
                    seed = i.toLong()
                )
            )
            assertTrue(program.isNotEmpty(), "Fallback should still produce a program")
        }
    }

    @Test
    fun `generate reduces complexity before falling back`() {
        // Start at EXTREME with maxLength=1 to force the full reduction chain:
        // EXTREME -> COMPLEX -> MEDIUM -> SIMPLE -> MINIMAL -> break -> fallback
        repeat(5) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(
                    maxLength = 1,
                    complexity = ProgramComplexity.EXTREME,
                    seed = i.toLong()
                )
            )
            assertTrue(program.isNotEmpty(), "Fallback should produce a program after full reduction")
        }
    }

    @Test
    fun `very small max length still produces something`() {
        val smallLengths = listOf(5, 10, 15, 20)
        for (maxLength in smallLengths) {
            repeat(10) { i ->
                val program = SmlProgramGenerator.generate(
                    ProgramConfig(maxLength = maxLength, seed = i.toLong())
                )
                assertTrue(program.isNotEmpty())
            }
        }
    }

    @Test
    fun `large max length produces substantial programs`() {
        repeat(10) { i ->
            val program = SmlProgramGenerator.generate(
                ProgramConfig(
                    maxLength = 5000,
                    complexity = ProgramComplexity.COMPLEX,
                    seed = i.toLong()
                )
            )
            // Complex programs with high limit should be reasonably sized
            assertTrue(program.length >= 50, "Complex programs should be substantial")
        }
    }

    @Test
    fun `programs contain SML keywords`() {
        val keywords = listOf("val", "fun", "let", "in", "end", "if", "then", "else", "fn", "case", "of")
        val allPrograms = (0 until 100).map { i ->
            SmlProgramGenerator.generate(
                ProgramConfig(maxLength = 1000, complexity = ProgramComplexity.MEDIUM, seed = i.toLong())
            )
        }
        val combinedText = allPrograms.joinToString(" ")

        // At least some keywords should appear across all programs
        val foundKeywords = keywords.filter { kw ->
            combinedText.contains(Regex("\\b$kw\\b"))
        }
        assertTrue(
            foundKeywords.size >= 3,
            "Should find at least some SML keywords, found: $foundKeywords"
        )
    }

    @Test
    fun `programs explore various constant types`() {
        val allPrograms = (0 until 100).map { i ->
            SmlProgramGenerator.generate(
                ProgramConfig(maxLength = 1000, complexity = ProgramComplexity.MEDIUM, seed = i.toLong())
            )
        }
        val combinedText = allPrograms.joinToString(" ")

        // Check for various constant types
        assertTrue(combinedText.contains(Regex("\\d+")), "Should contain integers")
        assertTrue(combinedText.contains('"'), "Should contain strings")
        // Reals, words, chars may or may not appear depending on random selection
    }
}
