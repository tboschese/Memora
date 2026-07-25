package com.memora.app.data

import com.memora.core.security.AutoLockController
import com.memora.core.security.PinVault
import com.memora.core.security.fake.InMemorySecurityStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercita o gate real (PinVault sobre store em memória) sem device: prova que setup/unlock abrem a
 * sessão com a chave certa, que o PIN errado não abre nada, e que a chave é **zerada** após uso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityPinGateTest {

    /** Sessão fake: guarda uma cópia da chave recebida para o teste inspecionar. */
    private class RecordingSession : EncryptedSession {
        var openedWith: ByteArray? = null
        var openCount = 0
        var closeCount = 0
        override fun open(key: ByteArray) {
            openCount++
            openedWith = key.copyOf() // cópia: o gate zera o original em seguida
        }
        override fun close() { closeCount++ }
    }

    private fun gate(
        session: EncryptedSession,
        autoLock: AutoLockController = AutoLockController(timeoutMs = 60_000),
        clock: () -> Long = { 1_000L },
    ): Pair<SecurityPinGate, PinVault> {
        // iterations baixo: teste rápido, sem device.
        val vault = PinVault(InMemorySecurityStore(), iterations = 1_000)
        val g = SecurityPinGate(
            vault = vault,
            autoLock = autoLock,
            session = session,
            clock = clock,
            derivationDispatcher = UnconfinedTestDispatcher(),
        )
        return g to vault
    }

    @Test
    fun `setup opens the session and unlocks reading`() = runTest {
        val session = RecordingSession()
        val autoLock = AutoLockController(timeoutMs = 60_000)
        val (g, _) = gate(session, autoLock)

        assertFalse(g.isConfigured())
        g.setup("2468".toCharArray())

        assertTrue(g.isConfigured())
        assertEquals(1, session.openCount)
        assertTrue(autoLock.isUnlocked)
    }

    @Test
    fun `unlock with the right pin opens with the same key setup derived`() = runTest {
        val setupSession = RecordingSession()
        val (g, vault) = gate(setupSession)
        g.setup("2468".toCharArray())
        val setupKey = setupSession.openedWith!!

        // Novo gate compartilhando o mesmo vault (mesmo store) simula uma sessão posterior.
        val unlockSession = RecordingSession()
        val autoLock = AutoLockController(timeoutMs = 60_000)
        val g2 = SecurityPinGate(vault, autoLock, unlockSession, clock = { 5_000L }, derivationDispatcher = UnconfinedTestDispatcher())

        assertTrue(g2.unlock("2468".toCharArray()))
        assertArrayEquals(setupKey, unlockSession.openedWith)
        assertTrue(autoLock.isUnlocked)
    }

    @Test
    fun `wrong pin does not open the session nor unlock`() = runTest {
        val setupSession = RecordingSession()
        val (g, vault) = gate(setupSession)
        g.setup("2468".toCharArray())

        val unlockSession = RecordingSession()
        val autoLock = AutoLockController(timeoutMs = 60_000)
        val g2 = SecurityPinGate(vault, autoLock, unlockSession, derivationDispatcher = UnconfinedTestDispatcher())

        assertFalse(g2.unlock("0000".toCharArray()))
        assertEquals(0, unlockSession.openCount)
        assertFalse(autoLock.isUnlocked)
    }

    @Test
    fun `the key handed to open is zeroed after the gate returns`() = runTest {
        // A sessão guarda o array ORIGINAL (não uma cópia) para observar o zeramento do gate.
        var original: ByteArray? = null
        val leakySession = object : EncryptedSession {
            override fun open(key: ByteArray) { original = key }
            override fun close() {}
        }
        val (g, _) = gate(leakySession)

        g.setup("2468".toCharArray())

        assertNull(original?.firstOrNull { it.toInt() != 0 }) // tudo zero
    }
}
