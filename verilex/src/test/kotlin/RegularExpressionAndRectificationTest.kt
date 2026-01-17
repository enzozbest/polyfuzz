package test

import lexer.Injector
import rexp.*
import value.*

object RegularExpressionAndRectificationTest {
    fun zeroAndOneBasics() {
        require(!ZERO.nullable())
        require(ONE.nullable())
        require(ZERO.der('x') == ZERO)
        require(ONE.der('x') == ZERO)
        require(ZERO.simp().first == ZERO)
        require(ONE.simp().first == ONE)
        // ONE.mkeps is Empty; ZERO.mkeps throws
        require(ONE.mkeps() == Empty)
        var threw = false
        try { ZERO.mkeps() } catch (_: IllegalStateException) { threw = true }
        require(threw)
    }

    fun charAndRangeThrowBeforeLowering() {
        val ch = CHAR('a')
        val rng = RANGE(setOf('a','b'))
        fun expectThrow(block: () -> Unit) {
            var ok = false
            try { block() } catch (_: IllegalStateException) { ok = true }
            require(ok)
        }
        expectThrow { ch.nullable() }
        expectThrow { ch.der('a') }
        expectThrow { ch.simp() }
        expectThrow { ch.mkeps() }
        expectThrow { rng.nullable() }
        expectThrow { rng.der('a') }
        expectThrow { rng.simp() }
        expectThrow { rng.mkeps() }
    }

    fun cfunAndLowering() {
        val cfun = CHAR('a').toCharFunctionFormat() as CFUN
        require(!cfun.nullable())
        require(cfun.der('a') == ONE)
        require(cfun.der('b') == ZERO)
        require(cfun.simp().first == cfun)
        // RANGE lowering
        val letters = RANGE(('a'..'z').toSet()).toCharFunctionFormat()
        require(letters.der('q') == ONE)
        require(letters.der('0') == ZERO)
    }

    fun altSimplificationBranches() {
        val a = CHAR('a').toCharFunctionFormat()
        val b = CHAR('b').toCharFunctionFormat()
        // r1s == ZERO -> right
        val (rA, fA) = ALT(ZERO, a).simp()
        require(rA == a)
        require(fA(Empty) is Right)
        // r2s == ZERO -> left
        val (rB, fB) = ALT(a, ZERO).simp()
        require(rB == a)
        require(fB(Empty) is Left)
        // r1s == r2s -> left(f1)
        val (rC, fC) = ALT(a, a).simp()
        require(rC == a)
        require(fC(Empty) is Left)
        // general alt
        val (rD, fD) = ALT(a, b).simp()
        require(rD is ALT)
        require(fD(Left(Empty)) is Left)
        require(fD(Right(Empty)) is Right)
        var threw = false
        try { fD(Empty) } catch (_: IllegalArgumentException) { threw = true }
        require(threw)
    }

    fun seqSimplificationBranches() {
        val a = CHAR('a').toCharFunctionFormat()
        val b = CHAR('b').toCharFunctionFormat()
        // r1s == ZERO
        val (r1, f1) = SEQ(ZERO, a).simp()
        require(r1 == ZERO)
        var threw = false
        try { f1(Empty) } catch (_: Exception) { threw = true }
        require(threw)
        // r2s == ZERO
        val (r2, f2) = SEQ(a, ZERO).simp()
        require(r2 == ZERO)
        threw = false
        try { f2(Empty) } catch (_: Exception) { threw = true }
        require(threw)
        // r1s == ONE -> seq_Empty1
        val (r3, f3) = SEQ(ONE, a).simp()
        require(r3 == a)
        require(f3(Empty) is Seq)
        // r2s == ONE -> seq_Empty2
        val (r4, f4) = SEQ(a, ONE).simp()
        require(r4 == a)
        require(f4(Empty) is Seq)
        // general seq
        val (r5, f5) = SEQ(a, b).simp()
        require(r5 is SEQ)
        val seqVal = f5(Seq(Empty, Empty))
        require(seqVal is Seq)
        threw = false
        try { f5(Left(Empty)) } catch (_: IllegalArgumentException) { threw = true }
        require(threw)
    }

    fun starAndPlusBasics() {
        val a = CHAR('a').toCharFunctionFormat()
        val star = STAR(a)
        val plus = PLUS(a)
        require(star.nullable())
        require(star.der('a') is SEQ)
        require(star.simp().first == star)
        require(star.mkeps() is Stars)
        require(plus.nullable() == a.nullable())
        require(plus.der('a') is SEQ)
        require(plus.simp().first == plus)
        require(plus.mkeps() is Plus)
    }

    fun recdBasics() {
        val a = CHAR('a').toCharFunctionFormat()
        val r = RECD("x", a)
        require(!r.nullable())
        require(r.der('a') == a.der('a'))
        require(r.mkeps() == Rec("x", Empty))
    }

    fun valueFlattenAndEnv() {
        val v: Value = Seq(
            Left(Rec("x", Seq(Chr('h'), Chr('i')))),
            Right(Stars(listOf(Chr('!'))))
        )
        require(v.flatten() == "hi!")
        require(v.env() == listOf("x" to "hi"))
    }

    fun rectificationHelpersThrowing() {
        var threw = false
        try { Rectification.ERROR(Empty) } catch (_: Exception) { threw = true }
        require(threw)
        threw = false
        try { Rectification.seq(Rectification.id, Rectification.id)(Left(Empty)) } catch (_: IllegalArgumentException) { threw = true }
        require(threw)
        threw = false
        try { Rectification.recd(Rectification.id)(Empty) } catch (_: IllegalArgumentException) { threw = true }
        require(threw)
    }

    fun injectorBranches() {
        val a = CHAR('a').toCharFunctionFormat()
        val b = CHAR('b').toCharFunctionFormat()
        // CFUN
        require(Injector.inj(a, 'a', Empty) == Chr('a'))
        // RECD
        val rec = RECD("t", a)
        require(Injector.inj(rec, 'a', Empty) == Rec("t", Chr('a')))
        // ALT Left/Right
        val alt = ALT(a, b)
        require(Injector.inj(alt, 'a', Left(Empty)) == Left(Chr('a')))
        require(Injector.inj(alt, 'b', Right(Empty)) == Right(Chr('b')))
        // SEQ basic
        val seq = SEQ(a, b)
        require(Injector.inj(seq, 'a', Seq(Empty, Empty)) == Seq(Chr('a'), Empty))
        // SEQ with Left(Seq(...))
        val leftSeq = Left(Seq(Empty, Empty))
        require(Injector.inj(seq, 'a', leftSeq) == Seq(Chr('a'), Empty))
        // SEQ with Right — need r1.mkeps
        val seq2 = SEQ(ONE, b)
        val injRight = Injector.inj(seq2, 'b', Right(Empty))
        require(injRight == Seq(Empty, Chr('b')))
        // STAR branch
        val star = STAR(a)
        val starVal = Injector.inj(star, 'a', Seq(Empty, Stars(emptyList())))
        require(starVal == Stars(listOf(Chr('a'))))
        // PLUS branch
        val plus = PLUS(a)
        val plusVal = Injector.inj(plus, 'a', Seq(Empty, Stars(emptyList())))
        require(plusVal == Plus(listOf(Chr('a'))))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        zeroAndOneBasics()
        charAndRangeThrowBeforeLowering()
        cfunAndLowering()
        altSimplificationBranches()
        seqSimplificationBranches()
        starAndPlusBasics()
        recdBasics()
        valueFlattenAndEnv()
        rectificationHelpersThrowing()
        injectorBranches()
    }
}
