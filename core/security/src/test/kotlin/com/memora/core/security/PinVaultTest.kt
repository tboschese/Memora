package com.memora.core.security

import com.memora.core.security.fake.InMemorySecurityStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinVaultTest {

    private fun newVault() = PinVault(InMemorySecurityStore(), iterations = 1_000)

    @Test
    fun `setup depois unlock com pin correto devolve a mesma chave`() {
        val vault = newVault()
        val keyOnSetup = vault.setup("1234".toCharArray())
        assertTrue(vault.isInitialized())

        val keyOnUnlock = vault.unlock("1234".toCharArray())
        assertArrayEquals(keyOnSetup, keyOnUnlock)
    }

    @Test
    fun `unlock com pin errado devolve null`() {
        val vault = newVault()
        vault.setup("1234".toCharArray())
        assertNull(vault.unlock("0000".toCharArray()))
    }

    @Test
    fun `vault nao inicializado nao destranca`() {
        val vault = newVault()
        assertFalse(vault.isInitialized())
        assertNull(vault.unlock("1234".toCharArray()))
    }

    @Test
    fun `clear remove o setup`() {
        val vault = newVault()
        vault.setup("1234".toCharArray())
        vault.clear()
        assertFalse(vault.isInitialized())
        assertNull(vault.unlock("1234".toCharArray()))
    }
}
