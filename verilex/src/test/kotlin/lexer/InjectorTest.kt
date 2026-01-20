@file:Suppress("ktlint:standard:no-wildcard-imports")

package lexer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rexp.*
import value.*

class InjectorTest {
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

        // Additional branch coverage: multi-part conditions failing parts individually

        // r is STAR && v is Seq && v.v2 is Stars
        // 1. r is NOT STAR
        // Actually, if r is CFUN (a) and v is Seq, it hits "r is SEQ && v is Seq" if r was SEQ,
        // but here r is CFUN, so it skips STAR, PLUS, SEQ, SEQ, SEQ, ALT, ALT and hits CFUN -> Chr(c).
        // So Injector.inj(a, 'a', Seq(Empty, Stars(emptyList()))) should NOT throw, it should return Chr('a').
        assertEquals(Chr('a'), Injector.inj(a, 'a', Seq(Empty, Stars(emptyList()))))
        // 2. v is NOT Seq
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(STAR(a), 'a', Stars(emptyList())) }
        // 3. v.v2 is NOT Stars
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(STAR(a), 'a', Seq(Empty, Empty)) }

        // r is PLUS && v is Seq && v.v2 is Stars
        // 1. r is NOT PLUS (already covered by STAR case above, but let's be explicit if needed)
        // 2. v is NOT Seq
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(PLUS(a), 'a', Stars(emptyList())) }
        // 3. v.v2 is NOT Stars
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(PLUS(a), 'a', Seq(Empty, Empty)) }

        // r is SEQ && v is Seq
        // 1. r is NOT SEQ (covered)
        // 2. v is NOT Seq (covered by Left/Right cases)

        // r is SEQ && v is Left && v.v is Seq
        // 1. r is SEQ, v is Left, v.v is NOT Seq
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(SEQ(a, b), 'a', Left(Empty)) }
        // 2. r is SEQ, v is NOT Left (falls through to Right check or else)
        // Covered by v is Seq or v is Right or v is Stars (throws)

        // r is SEQ && v is Right
        // 1. r is SEQ, v is Right (covered)
        // 2. r is SEQ, v is neither Seq, nor Left with Seq, nor Right
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(SEQ(a, b), 'a', Stars(emptyList())) }

        // Also test r is SEQ but v is Left with Seq - this should pass and cover the branch
        assertEquals(Seq(Chr('a'), Empty), Injector.inj(SEQ(a, b), 'a', Left(Seq(Empty, Empty))))

        // r is ALT && v is Left
        // r is ALT && v is Right
        // 1. r is ALT, v is neither Left nor Right
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(ALT(a, b), 'a', Empty) }

        // else branch
        assertThrows(IllegalArgumentException::class.java) { Injector.inj(ONE, 'a', Empty) }
    }
}
