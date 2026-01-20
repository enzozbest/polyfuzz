package value

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ValueTest {
    @Test
    fun testEmpty() {
        val v = Empty
        assertEquals("", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())
    }

    @Test
    fun testChr() {
        val v = Chr('a')
        assertEquals("a", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())
    }

    @Test
    fun testLeft() {
        val v = Left(Chr('a'))
        assertEquals("a", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())

        val vNested = Left(Rec("tag", Chr('a')))
        assertEquals("a", vNested.flatten())
        assertEquals(listOf("tag" to "a"), vNested.env())
    }

    @Test
    fun testRight() {
        val v = Right(Chr('b'))
        assertEquals("b", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())

        val vNested = Right(Rec("tag", Chr('b')))
        assertEquals("b", vNested.flatten())
        assertEquals(listOf("tag" to "b"), vNested.env())
    }

    @Test
    fun testSeq() {
        val v = Seq(Chr('a'), Chr('b'))
        assertEquals("ab", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())

        val vTagged = Seq(Rec("t1", Chr('a')), Rec("t2", Chr('b')))
        assertEquals("ab", vTagged.flatten())
        assertEquals(listOf("t1" to "a", "t2" to "b"), vTagged.env())

        val vMixed = Seq(Rec("t1", Chr('a')), Chr('b'))
        assertEquals(listOf("t1" to "a"), vMixed.env())
    }

    @Test
    fun testStars() {
        val v = Stars(listOf(Chr('a'), Chr('b'), Chr('c')))
        assertEquals("abc", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())

        val vTagged = Stars(listOf(Rec("t1", Chr('a')), Rec("t2", Chr('b'))))
        assertEquals("ab", vTagged.flatten())
        assertEquals(listOf("t1" to "a", "t2" to "b"), vTagged.env())

        val vEmpty = Stars(emptyList())
        assertEquals("", vEmpty.flatten())
        assertEquals(emptyList<Pair<String, String>>(), vEmpty.env())
    }

    @Test
    fun testPlus() {
        val v = Plus(listOf(Chr('a'), Chr('b')))
        assertEquals("ab", v.flatten())
        assertEquals(emptyList<Pair<String, String>>(), v.env())

        val vTagged = Plus(listOf(Rec("t1", Chr('a')), Rec("t2", Chr('b'))))
        assertEquals("ab", vTagged.flatten())
        assertEquals(listOf("t1" to "a", "t2" to "b"), vTagged.env())
    }

    @Test
    fun testRec() {
        val v = Rec("tag", Chr('a'))
        assertEquals("a", v.flatten())
        assertEquals(listOf("tag" to "a"), v.env())

        val vNested = Rec("outer", Rec("inner", Chr('x')))
        assertEquals("x", vNested.flatten())
        // Rec.env() returns listOf(x to v.flatten()), so it doesn't include inner tags
        // Let's verify this behavior in Value.kt
        assertEquals(listOf("outer" to "x"), vNested.env())
    }

    @Test
    fun testComplexEnv() {
        val v =
            Seq(
                Rec("token1", Seq(Chr('a'), Chr('b'))),
                Stars(
                    listOf(
                        Rec("token2", Chr('c')),
                        Rec("token2", Chr('d')),
                    ),
                ),
            )
        assertEquals("abcd", v.flatten())
        assertEquals(
            listOf(
                "token1" to "ab",
                "token2" to "c",
                "token2" to "d",
            ),
            v.env(),
        )
    }

    @Test
    fun testDeeplyNestedRec() {
        val v = Rec("tag1", Seq(Rec("tag2", Chr('a')), Chr('b')))
        assertEquals("ab", v.flatten())
        // Currently Rec.env() returns listOf(x to v.flatten())
        // which means it does NOT recurse into its children for more tags.
        assertEquals(listOf("tag1" to "ab"), v.env())
    }
}
