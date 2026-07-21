package com.memora.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KeyDerivationTest {

    private val salt = ByteArray(16) { it.toByte() }

    @Test
    fun `mesmo pin e salt produzem a mesma chave (deterministico)`() {
        val a = KeyDerivation.deriveKey("1234".toCharArray(), salt, iterations = 1_000)
        val b = KeyDerivation.deriveKey("1234".toCharArray(), salt, iterations = 1_000)
        assertArrayEquals(a, b)
        assertEquals(32, a.size) // 256 bits
    }

    @Test
    fun `salt diferente muda a chave`() {
        val a = KeyDerivation.deriveKey("1234".toCharArray(), salt, iterations = 1_000)
        val other = ByteArray(16) { (it + 1).toByte() }
        val b = KeyDerivation.deriveKey("1234".toCharArray(), other, iterations = 1_000)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `pin diferente muda a chave`() {
        val a = KeyDerivation.deriveKey("1234".toCharArray(), salt, iterations = 1_000)
        val b = KeyDerivation.deriveKey("9999".toCharArray(), salt, iterations = 1_000)
        assertFalse(a.contentEquals(b))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `salt vazio e rejeitado`() {
        KeyDerivation.deriveKey("1234".toCharArray(), ByteArray(0))
    }
}
