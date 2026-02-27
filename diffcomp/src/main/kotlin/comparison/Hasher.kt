package comparison

import java.security.MessageDigest

object Hasher {
    private val digest = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }

    fun hash(input: String): ByteArray =
        digest.get().let { md ->
            md.reset()
            md.digest(input.toByteArray(Charsets.UTF_8))
        }

    fun equal(a: String, b: String): Boolean = hash(a).contentEquals(hash(b))
}
