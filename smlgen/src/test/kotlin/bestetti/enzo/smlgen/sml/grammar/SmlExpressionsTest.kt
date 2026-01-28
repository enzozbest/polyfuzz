package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.GenerationConfig
import bestetti.enzo.smlgen.gen.GenerationContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for SmlExpressions generators.
 */
class SmlExpressionsTest {

    private fun ctx(seed: Long = 42L, maxDepth: Int = 5, maxRepeat: Int = 3) = GenerationContext(
        random = Random(seed),
        config = GenerationConfig(maxDepth = maxDepth, maxRepeat = maxRepeat)
    )

    @Test
    fun `constExpr generates constants`() {
        repeat(50) { i ->
            val expr = SmlExpressions.constExpr(ctx(i.toLong()))
            assertTrue(expr.isNotEmpty(), "Const expression should not be empty")
        }
    }

    @Test
    fun `varExpr generates variable expressions`() {
        repeat(50) { i ->
            val expr = SmlExpressions.varExpr(ctx(i.toLong()))
            assertTrue(expr.isNotEmpty(), "Var expression should not be empty")
        }
    }

    @Test
    fun `opExpr generates op prefixed expressions`() {
        repeat(50) { i ->
            val expr = SmlExpressions.opExpr(ctx(i.toLong()))
            assertTrue(expr.startsWith("op"), "Op expression should start with 'op': $expr")
        }
    }

    @Test
    fun `unitExpr generates unit expression`() {
        val expr = SmlExpressions.unitExpr(ctx())
        assertTrue(expr == "()", "Unit expression should be '()': $expr")
    }

    @Test
    fun `selectorExpr generates record selector`() {
        repeat(50) { i ->
            val expr = SmlExpressions.selectorExpr(ctx(i.toLong()))
            assertTrue(expr.startsWith("#"), "Selector should start with '#': $expr")
        }
    }

    @Test
    fun `letExpr generates let expressions`() {
        repeat(30) { i ->
            val expr = SmlExpressions.letExpr(ctx(i.toLong()))
            assertTrue(expr.startsWith("let"), "Let expression should start with 'let': $expr")
            assertTrue(expr.contains("in"), "Let expression should contain 'in': $expr")
            assertTrue(expr.endsWith("end"), "Let expression should end with 'end': $expr")
        }
    }

    @Test
    fun `atomicExpression generates valid expressions`() {
        repeat(50) { i ->
            val expr = SmlExpressions.atomicExpression(ctx(i.toLong()))
            assertTrue(expr.isNotEmpty(), "Atomic expression should not be empty")
        }
    }

    @Test
    fun `expression generates valid expressions`() {
        repeat(50) { i ->
            val expr = SmlExpressions.expression(ctx(i.toLong()))
            assertTrue(expr.isNotEmpty(), "Expression should not be empty")
        }
    }

    @Test
    fun `simpleExpression generates valid expressions`() {
        repeat(50) { i ->
            val expr = SmlExpressions.simpleExpression(ctx(i.toLong()))
            assertTrue(expr.isNotEmpty(), "Simple expression should not be empty")
        }
    }

    @Test
    fun `expression produces variety`() {
        val exprs = (0L until 100L).map { SmlExpressions.expression(ctx(it)) }.toSet()
        assertTrue(exprs.size > 20, "Should produce varied expressions")
    }

    @Test
    fun `atomicExpression produces variety`() {
        val exprs = (0L until 100L).map { SmlExpressions.atomicExpression(ctx(it)) }.toSet()
        assertTrue(exprs.size > 20, "Should produce varied atomic expressions")
    }

    @Test
    fun `simpleExpression produces variety`() {
        val exprs = (0L until 100L).map { SmlExpressions.simpleExpression(ctx(it)) }.toSet()
        assertTrue(exprs.size > 20, "Should produce varied simple expressions")
    }

    @Test
    fun `generation is deterministic with same seed`() {
        val result1 = SmlExpressions.expression(ctx(12345L))
        val result2 = SmlExpressions.expression(ctx(12345L))
        assertTrue(result1 == result2, "Same seed should produce same result")
    }
}
