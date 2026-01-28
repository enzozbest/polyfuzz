package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for SmlDeclarations generators.
 */
class SmlDeclarationsTest {

    private fun ctx(seed: Long = 42L, maxDepth: Int = 5, maxRepeat: Int = 3) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = maxDepth, maxRepeat = maxRepeat)
    )

    @Test
    fun `valDec generates val declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.valDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("val"), "Val declaration should start with 'val': $decl")
        }
    }

    @Test
    fun `valRecDec generates val rec declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.valRecDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("val"), "Val rec declaration should start with 'val': $decl")
            assertTrue(decl.contains("rec"), "Val rec declaration should contain 'rec': $decl")
        }
    }

    @Test
    fun `funDec generates fun declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.funDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("fun"), "Fun declaration should start with 'fun': $decl")
        }
    }

    @Test
    fun `typeDec generates type declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.typeDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("type"), "Type declaration should start with 'type': $decl")
        }
    }

    @Test
    fun `datatypeDec generates datatype declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.datatypeDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("datatype"), "Datatype declaration should start with 'datatype': $decl")
        }
    }

    @Test
    fun `datatypeRepDec generates datatype replication declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.datatypeRepDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("datatype"), "Datatype rep should start with 'datatype': $decl")
            assertTrue(
                decl.contains("datatype") && decl.indexOf("datatype") != decl.lastIndexOf("datatype"),
                "Should contain 'datatype' twice: $decl"
            )
        }
    }

    @Test
    fun `exceptionDec generates exception declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.exceptionDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("exception"), "Exception declaration should start with 'exception': $decl")
        }
    }

    @Test
    fun `localDec generates local declarations`() {
        repeat(30) { i ->
            val decl = SmlDeclarations.localDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("local"), "Local declaration should start with 'local': $decl")
            assertTrue(decl.contains("in"), "Local declaration should contain 'in': $decl")
            assertTrue(decl.endsWith("end"), "Local declaration should end with 'end': $decl")
        }
    }

    @Test
    fun `infixDec generates infix declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.infixDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("infix"), "Infix declaration should start with 'infix': $decl")
        }
    }

    @Test
    fun `infixrDec generates infixr declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.infixrDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("infixr"), "Infixr declaration should start with 'infixr': $decl")
        }
    }

    @Test
    fun `nonfixDec generates nonfix declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.nonfixDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("nonfix"), "Nonfix declaration should start with 'nonfix': $decl")
        }
    }

    @Test
    fun `openDec generates open declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.openDec(ctx(i.toLong()))
            assertTrue(decl.startsWith("open"), "Open declaration should start with 'open': $decl")
        }
    }

    @Test
    fun `declaration generates valid declarations`() {
        repeat(50) { i ->
            val decl = SmlDeclarations.declaration(ctx(i.toLong()))
            assertTrue(decl.isNotEmpty(), "Declaration should not be empty")
        }
    }

    @Test
    fun `declarations generates sequences of declarations`() {
        repeat(50) { i ->
            val decls = SmlDeclarations.declarations(ctx(i.toLong()))
            assertTrue(decls.isNotEmpty(), "Declarations should not be empty")
        }
    }

    @Test
    fun `emptyDec generates empty string`() {
        val decl = SmlDeclarations.emptyDec(ctx())
        assertTrue(decl.isEmpty(), "Empty declaration should be empty")
    }

    @Test
    fun `declaration produces variety`() {
        val decls = (0L until 100L).map { SmlDeclarations.declaration(ctx(it)) }.toSet()
        assertTrue(decls.size > 20, "Should produce varied declarations")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlDeclarations.valDec(ctx(12345L))
        val result2 = SmlDeclarations.valDec(ctx(12345L))
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
