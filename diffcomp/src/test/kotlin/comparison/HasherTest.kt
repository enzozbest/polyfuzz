package comparison

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasherTest {

    @Test
    fun `identical strings produce equal hashes`() {
        assertTrue(Hasher.equal("VAL ID(x)", "VAL ID(x)"))
    }

    @Test
    fun `different strings produce unequal hashes`() {
        assertFalse(Hasher.equal("VAL ID(x)", "VAL ID(y)"))
    }

    @Test
    fun `empty strings are equal`() {
        assertTrue(Hasher.equal("", ""))
    }

    @Test
    fun `hash produces 32 bytes`() {
        assertEquals(32, Hasher.hash("test").size)
    }
}
