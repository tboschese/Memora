package com.memora.app.session

import com.memora.core.security.AutoLockController
import com.memora.feature.onboarding.PinGate
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCoordinatorTest {

    /** Gate fake: só o `isConfigured` importa aqui; o unlock real é coberto no `SecurityPinGateTest`. */
    private class FakeGate(var configured: Boolean) : PinGate {
        override fun isConfigured() = configured
        override suspend fun setup(pin: CharArray) { configured = true }
        override suspend fun unlock(pin: CharArray) = true
    }

    /** Relógio controlado pelo teste (o auto-lock recebe o tempo por parâmetro). */
    private class Clock(var now: Long = 0L) : () -> Long {
        override fun invoke() = now
    }

    private fun coordinator(
        configured: Boolean,
        timeoutMs: Long = 1_000L,
        clock: Clock = Clock(),
    ): Triple<SessionCoordinator, AutoLockController, Clock> {
        val autoLock = AutoLockController(timeoutMs)
        val c = SessionCoordinator(FakeGate(configured), autoLock, clock)
        return Triple(c, autoLock, clock)
    }

    @Test
    fun `no pin configured starts in onboarding`() {
        val (c, _, _) = coordinator(configured = false)
        assertEquals(SessionPhase.ONBOARDING, c.phase.value)
    }

    @Test
    fun `configured but not authenticated is locked`() {
        val (c, _, _) = coordinator(configured = true)
        assertEquals(SessionPhase.LOCKED, c.phase.value)
    }

    @Test
    fun `authenticating unlocks reading`() {
        val (c, autoLock, clock) = coordinator(configured = true)

        autoLock.unlock(clock.now) // o que o gate faz internamente ao acertar o PIN
        c.onAuthenticated()

        assertEquals(SessionPhase.UNLOCKED, c.phase.value)
    }

    @Test
    fun `auto-lock timeout relocks on refresh`() {
        val clock = Clock(now = 0L)
        val (c, autoLock, _) = coordinator(configured = true, timeoutMs = 1_000L, clock = clock)
        autoLock.unlock(0L)
        c.onAuthenticated()
        assertEquals(SessionPhase.UNLOCKED, c.phase.value)

        clock.now = 1_000L // atingiu o timeout
        c.refresh()

        assertEquals(SessionPhase.LOCKED, c.phase.value)
    }

    @Test
    fun `activity defers the auto-lock`() {
        val clock = Clock(now = 0L)
        val (c, autoLock, _) = coordinator(configured = true, timeoutMs = 1_000L, clock = clock)
        autoLock.unlock(0L)
        c.onAuthenticated()

        clock.now = 800L
        c.onActivity()   // adia: última atividade agora é 800
        clock.now = 1_500L // 700ms desde a atividade < timeout
        c.refresh()

        assertEquals(SessionPhase.UNLOCKED, c.phase.value)
    }

    @Test
    fun `manual lock returns to locked`() {
        val (c, autoLock, clock) = coordinator(configured = true)
        autoLock.unlock(clock.now)
        c.onAuthenticated()

        c.lock()

        assertEquals(SessionPhase.LOCKED, c.phase.value)
    }
}
