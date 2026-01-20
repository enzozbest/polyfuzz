@file:Suppress("ktlint:standard:no-wildcard-imports")

package rexp

import lexer.Injector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import value.*

class RegularExpressionTest {
    @Test
    fun testZERO() {
        val r = ZERO
        assertEquals("ZERO", r.toString())
        assertFalse(r.nullable())
        assertEquals(ZERO, r.der('a'))

        val (rs, rect) = r.simp()
        assertEquals(ZERO, rs)
        assertEquals(Empty, rect(Empty)) // id
        assertEquals(ZERO, r.toCharFunctionFormat())
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testONE() {
        val r = ONE
        assertEquals("ONE", r.toString())
        assertTrue(r.nullable())
        assertEquals(ZERO, r.der('a'))

        val (rs, rect) = r.simp()
        assertEquals(ONE, rs)
        assertEquals(Empty, rect(Empty)) // id
        assertEquals(ONE, r.toCharFunctionFormat())
        assertEquals(Empty, r.mkeps())
    }

    @Test
    fun testCHAR() {
        val r = CHAR('a')
        assertEquals("[a]", r.toString())
        val lowered = r.toCharFunctionFormat()
        assertTrue(lowered is CFUN)
        assertEquals("[a]", lowered.toString())

        // Before lowering, these should throw
        assertThrows(IllegalStateException::class.java) { r.nullable() }
        assertThrows(IllegalStateException::class.java) { r.der('a') }
        assertThrows(IllegalStateException::class.java) { r.simp() }
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testRANGE() {
        val r = RANGE(setOf('a', 'b'))
        assertEquals("[a,b]", r.toString())

        val lowered = r.toCharFunctionFormat()
        assertTrue(lowered is CFUN)
        assertEquals("[a,b]", lowered.toString())

        // Before lowering, these should throw
        assertThrows(IllegalStateException::class.java) { r.nullable() }
        assertThrows(IllegalStateException::class.java) { r.der('a') }
        assertThrows(IllegalStateException::class.java) { r.simp() }
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testCFUN() {
        val r = CFUN("test") { it == 'a' }
        assertEquals("test", r.toString())
        assertFalse(r.nullable())
        assertEquals(ONE, r.der('a'))
        assertEquals(ZERO, r.der('b'))

        val (rs, rect) = r.simp()
        assertEquals(r, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(r, r.toCharFunctionFormat())
        assertThrows(IllegalStateException::class.java) { r.mkeps() }
    }

    @Test
    fun testALT() {
        val a = CFUN("a") { it == 'a' }
        val b = CFUN("b") { it == 'b' }
        val alt = ALT(a, b)
        assertEquals("(a) | (b)", alt.toString())
        assertFalse(alt.nullable())
        assertTrue(ALT(ONE, a).nullable())
        assertTrue(ALT(a, ONE).nullable())

        assertEquals(ALT(ONE, ZERO), alt.der('a'))
        assertEquals(ALT(ZERO, ONE), alt.der('b'))

        assertEquals(Pair(a, Right(Empty)), ALT(ZERO, a).simp().let { it.first to it.second(Empty) })
        assertEquals(Pair(a, Left(Empty)), ALT(a, ZERO).simp().let { it.first to it.second(Empty) })

        val (r1, f1) = ALT(ZERO, a).simp()
        assertEquals(a, r1)
        assertEquals(Right(Empty), f1(Empty))

        val (r2, f2) = ALT(a, ZERO).simp()
        assertEquals(a, r2)
        assertEquals(Left(Empty), f2(Empty))

        val (r3, f3) = ALT(a, a).simp()
        assertEquals(a, r3)
        assertEquals(Left(Empty), f3(Empty))

        val (r4, f4) = ALT(a, b).simp()
        assertTrue(r4 is ALT)
        assertEquals(Left(Empty), f4(Left(Empty)))
        assertEquals(Right(Empty), f4(Right(Empty)))
        assertThrows(IllegalArgumentException::class.java) { f4(Empty) }

        assertEquals(Left(Empty), ALT(ONE, a).mkeps())
        assertEquals(Right(Empty), ALT(a, ONE).mkeps())

        val charAlt = ALT(CHAR('a'), CHAR('b'))
        val loweredAlt = charAlt.toCharFunctionFormat()
        assertTrue(loweredAlt is ALT)
        assertTrue((loweredAlt as ALT).r1 is CFUN)
        assertTrue(loweredAlt.r2 is CFUN)
    }

    @Test
    fun testSEQ() {
        val a = CFUN("a") { it == 'a' }
        val b = CFUN("b") { it == 'b' }
        val seq = SEQ(a, b)
        assertEquals("(a) ~ (b)", seq.toString())
        assertFalse(seq.nullable())
        assertTrue(SEQ(ONE, ONE).nullable())
        assertFalse(SEQ(ONE, a).nullable())

        // !r1.nullable()
        assertEquals(SEQ(ONE, b), seq.der('a'))

        // r1.nullable()
        val seqNull = SEQ(ONE, b)
        assertEquals(ALT(SEQ(ZERO, b), ONE), seqNull.der('b'))

        // simp branches
        assertEquals(ZERO, SEQ(ZERO, a).simp().first)
        assertThrows(Exception::class.java) { SEQ(ZERO, a).simp().second(Empty) }

        assertEquals(ZERO, SEQ(a, ZERO).simp().first)

        val (r1, f1) = SEQ(ONE, a).simp()
        assertEquals(a, r1)
        assertEquals(Seq(Empty, Empty), f1(Empty))

        val (r2, f2) = SEQ(a, ONE).simp()
        assertEquals(a, r2)
        assertEquals(Seq(Empty, Empty), f2(Empty))

        val (r3, f3) = SEQ(a, b).simp()
        assertTrue(r3 is SEQ)
        assertEquals(Seq(Empty, Empty), f3(Seq(Empty, Empty)))
        assertThrows(IllegalArgumentException::class.java) { f3(Empty) }

        assertEquals(Seq(Empty, Empty), SEQ(ONE, ONE).mkeps())

        val charSeq = SEQ(CHAR('a'), CHAR('b'))
        val loweredSeq = charSeq.toCharFunctionFormat()
        assertTrue(loweredSeq is SEQ)
        assertTrue((loweredSeq as SEQ).r1 is CFUN)
    }

    @Test
    fun testSTAR() {
        val a = CFUN("a") { it == 'a' }
        val star = STAR(a)
        assertEquals("(a)*", star.toString())
        assertTrue(star.nullable())
        assertEquals(SEQ(ONE, star), star.der('a'))

        val (rs, rect) = star.simp()
        assertEquals(star, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(Stars(emptyList()), star.mkeps())
        assertTrue(star.toCharFunctionFormat() is STAR)
    }

    @Test
    fun testPLUS() {
        val a = CFUN("a") { it == 'a' }
        val plus = PLUS(a)
        assertEquals("(a)+", plus.toString())
        assertFalse(plus.nullable())
        assertTrue(PLUS(ONE).nullable())
        assertEquals(SEQ(ONE, STAR(a)), plus.der('a'))

        val (rs, rect) = plus.simp()
        assertEquals(plus, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(Plus(listOf(Empty)), PLUS(ONE).mkeps())
        assertTrue(plus.toCharFunctionFormat() is PLUS)
    }

    @Test
    fun testRECD() {
        val a = CFUN("a") { it == 'a' }
        val recd = RECD("tag", a)
        assertEquals("(tag:a)", recd.toString())
        assertFalse(recd.nullable())
        assertTrue(RECD("tag", ONE).nullable())
        assertEquals(ONE, recd.der('a'))

        val (rs, rect) = recd.simp()
        assertEquals(recd, rs)
        assertEquals(Empty, rect(Empty))
        assertEquals(Rec("tag", Empty), RECD("tag", ONE).mkeps())
        assertTrue(recd.toCharFunctionFormat() is RECD)
    }

    @Test
    fun testInjector() {
        val a = CFUN("a") { it == 'a' }
        val b = CFUN("b") { it == 'b' }

        // STAR
        val star = STAR(a)
        val vStar = Seq(Empty, Stars(listOf(Chr('a'))))
        val resStar = Injector.inj(star, 'a', vStar)
        assertEquals(Stars(listOf(Chr('a'), Chr('a'))), resStar)

        // PLUS
        val plus = PLUS(a)
        val vPlus = Seq(Empty, Stars(listOf(Chr('a'))))
        val resPlus = Injector.inj(plus, 'a', vPlus)
        assertEquals(Plus(listOf(Chr('a'), Chr('a'))), resPlus)

        // SEQ
        val seq = SEQ(a, b)
        // v is Seq
        assertEquals(Seq(Chr('a'), Empty), Injector.inj(seq, 'a', Seq(Empty, Empty)))
        // v is Left && v.v is Seq
        assertEquals(Seq(Chr('a'), Empty), Injector.inj(seq, 'a', Left(Seq(Empty, Empty))))
        // v is Right
        val seq2 = SEQ(ONE, b)
        assertEquals(Seq(Empty, Chr('b')), Injector.inj(seq2, 'b', Right(Empty)))

        // ALT
        val alt = ALT(a, b)
        assertEquals(Left(Chr('a')), Injector.inj(alt, 'a', Left(Empty)))
        assertEquals(Right(Chr('b')), Injector.inj(alt, 'b', Right(Empty)))

        // CFUN
        assertEquals(Chr('a'), Injector.inj(a, 'a', Empty))

        // RECD
        val recd = RECD("t", a)
        assertEquals(Rec("t", Chr('a')), Injector.inj(recd, 'a', Empty))

        // Outer Rec in Injector.inj
        assertEquals(Rec("out", Chr('a')), Injector.inj(a, 'a', Rec("out", Empty)))

        // Internal Rec in injInternal
        assertEquals(Rec("out", Rec("in", Chr('a'))), Injector.inj(a, 'a', Rec("out", Rec("in", Empty))))

        // e.g. r is STAR and v is Left.
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(STAR(a), 'a', Left(Empty)) }
    }

    @Test
    fun testRegexConveniences() {
        val a = CHAR('a')
        val b = CHAR('b')

        // X
        assertTrue(a X b is ALT)
        assertTrue(a X "b" is ALT)
        assertTrue("a" X b is ALT)
        assertTrue("a" X "b" is ALT)

        // F
        assertTrue(a F b is SEQ)
        assertTrue(a F "b" is SEQ)
        assertTrue("a" F b is SEQ)
        assertTrue("a" F "b" is SEQ)

        // S
        assertTrue(a.S() is STAR)
        assertTrue("a".S() is STAR)

        // P
        assertTrue(a.P() is PLUS)
        assertTrue("a".P() is PLUS)

        // T
        assertTrue("tag" T a is RECD)
        assertTrue("tag" T "a" is RECD)

        // toRegex
        assertEquals(SEQ(CHAR('a'), CHAR('b')), "ab".toRegex())
        assertEquals(ONE, "".toRegex())
    }

    @Test
    fun testRectification() {
        val rRect = Rectification.recd(Rectification.id)
        assertEquals(Rec("x", Empty), rRect(Rec("x", Empty)))
        assertThrows(IllegalArgumentException::class.java) { rRect(Empty) }

        // alt
        val fAlt = Rectification.alt(Rectification.id, Rectification.id)
        assertEquals(Left(Empty), fAlt(Left(Empty)))
        assertEquals(Right(Empty), fAlt(Right(Empty)))
        assertThrows(IllegalArgumentException::class.java) { fAlt(Empty) }

        // seq
        val fSeq = Rectification.seq(Rectification.id, Rectification.id)
        assertEquals(Seq(Empty, Empty), fSeq(Seq(Empty, Empty)))
        assertThrows(IllegalArgumentException::class.java) { fSeq(Empty) }
    }
}
