package bestetti.enzo.smlgen

import bestetti.enzo.smlgen.sml.generator.ProgramComplexity
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class CliArgsTest {

    @Test
    fun `no arguments produces defaults`() {
        val args = CliArgs.parse(emptyArray())
        assertEquals(10, args.count)
        assertNull(args.seed)
        assertEquals(".", args.outputDir)
        assertEquals(Complexity.Mixed, args.complexity)
    }

    @Test
    fun `parse -n flag`() {
        val args = CliArgs.parse(arrayOf("-n", "25"))
        assertEquals(25, args.count)
    }

    @Test
    fun `parse -seed flag`() {
        val args = CliArgs.parse(arrayOf("-seed", "42"))
        assertEquals(42L, args.seed)
    }

    @Test
    fun `parse negative seed`() {
        val args = CliArgs.parse(arrayOf("-seed", "-999"))
        assertEquals(-999L, args.seed)
    }

    @Test
    fun `parse -o flag`() {
        val args = CliArgs.parse(arrayOf("-o", "/tmp/output"))
        assertEquals("/tmp/output", args.outputDir)
    }

    @Test
    fun `parse -c with each complexity level`() {
        for (complexity in ProgramComplexity.entries) {
            val args = CliArgs.parse(arrayOf("-c", complexity.name))
            assertEquals(Complexity.Single(complexity), args.complexity)
        }
    }

    @Test
    fun `parse -c MIXED`() {
        val args = CliArgs.parse(arrayOf("-c", "MIXED"))
        assertEquals(Complexity.Mixed, args.complexity)
    }

    @Test
    fun `complexity is case insensitive`() {
        val lowercase = CliArgs.parse(arrayOf("-c", "medium"))
        assertEquals(Complexity.Single(ProgramComplexity.MEDIUM), lowercase.complexity)

        val mixedCase = CliArgs.parse(arrayOf("-c", "Simple"))
        assertEquals(Complexity.Single(ProgramComplexity.SIMPLE), mixedCase.complexity)

        val mixedMixed = CliArgs.parse(arrayOf("-c", "mixed"))
        assertEquals(Complexity.Mixed, mixedMixed.complexity)
    }

    @Test
    fun `parse all flags together`() {
        val args = CliArgs.parse(arrayOf("-n", "5", "-seed", "123", "-o", "/out", "-c", "EXTREME"))
        assertEquals(5, args.count)
        assertEquals(123L, args.seed)
        assertEquals("/out", args.outputDir)
        assertEquals(Complexity.Single(ProgramComplexity.EXTREME), args.complexity)
    }

    @Test
    fun `flags can appear in any order`() {
        val args = CliArgs.parse(arrayOf("-c", "SIMPLE", "-o", "/dir", "-seed", "7", "-n", "3"))
        assertEquals(3, args.count)
        assertEquals(7L, args.seed)
        assertEquals("/dir", args.outputDir)
        assertEquals(Complexity.Single(ProgramComplexity.SIMPLE), args.complexity)
    }

    @Test
    fun `unknown argument fails`() {
        val e = assertFailsWith<IllegalStateException> {
            CliArgs.parse(arrayOf("-x"))
        }
        assertTrue(e.message!!.contains("Unknown argument"))
    }

    @Test
    fun `-n without value fails`() {
        assertFailsWith<IllegalArgumentException> {
            CliArgs.parse(arrayOf("-n"))
        }
    }

    @Test
    fun `-seed without value fails`() {
        assertFailsWith<IllegalArgumentException> {
            CliArgs.parse(arrayOf("-seed"))
        }
    }

    @Test
    fun `-o without value fails`() {
        assertFailsWith<IllegalArgumentException> {
            CliArgs.parse(arrayOf("-o"))
        }
    }

    @Test
    fun `-c without value fails`() {
        assertFailsWith<IllegalArgumentException> {
            CliArgs.parse(arrayOf("-c"))
        }
    }

    @Test
    fun `invalid number for -n fails`() {
        assertFailsWith<IllegalStateException> {
            CliArgs.parse(arrayOf("-n", "abc"))
        }
    }

    @Test
    fun `zero count fails`() {
        assertFailsWith<IllegalArgumentException> {
            CliArgs.parse(arrayOf("-n", "0"))
        }
    }

    @Test
    fun `negative count fails`() {
        assertFailsWith<IllegalArgumentException> {
            CliArgs.parse(arrayOf("-n", "-5"))
        }
    }

    @Test
    fun `invalid seed value fails`() {
        assertFailsWith<IllegalStateException> {
            CliArgs.parse(arrayOf("-seed", "not_a_number"))
        }
    }

    @Test
    fun `unknown complexity fails`() {
        val e = assertFailsWith<IllegalStateException> {
            CliArgs.parse(arrayOf("-c", "INVALID"))
        }
        assertTrue(e.message!!.contains("Unknown complexity"))
        assertTrue(e.message!!.contains("MINIMAL"))
    }

    @Test
    fun `-h throws HelpRequestedException`() {
        assertFailsWith<HelpRequestedException> {
            CliArgs.parse(arrayOf("-h"))
        }
    }

    @Test
    fun `--help throws HelpRequestedException`() {
        assertFailsWith<HelpRequestedException> {
            CliArgs.parse(arrayOf("--help"))
        }
    }

    @Test
    fun `data class equality`() {
        val a = CliArgs(count = 5, seed = 42L, outputDir = "/tmp", complexity = Complexity.Mixed)
        val b = CliArgs(count = 5, seed = 42L, outputDir = "/tmp", complexity = Complexity.Mixed)
        assertEquals(a, b)
    }

    @Test
    fun `Complexity Single equality`() {
        val a = Complexity.Single(ProgramComplexity.MEDIUM)
        val b = Complexity.Single(ProgramComplexity.MEDIUM)
        assertEquals(a, b)
    }
}
