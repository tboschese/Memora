package com.memora.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockControllerTest {

    @Test
    fun `destranca e mantem destrancado antes do timeout`() {
        val lock = AutoLockController(timeoutMs = 30_000)
        lock.unlock(nowMs = 0)
        assertTrue(lock.isUnlocked)
        assertTrue("dentro do timeout continua destrancado", lock.refresh(nowMs = 29_999))
    }

    @Test
    fun `tranca ao atingir o timeout de inatividade`() {
        val lock = AutoLockController(timeoutMs = 30_000)
        lock.unlock(nowMs = 0)
        assertFalse("no limite do timeout, tranca", lock.refresh(nowMs = 30_000))
        assertFalse(lock.isUnlocked)
    }

    @Test
    fun `atividade adia o auto-lock`() {
        val lock = AutoLockController(timeoutMs = 30_000)
        lock.unlock(nowMs = 0)
        lock.onActivity(nowMs = 20_000)
        // 40s desde o unlock, mas só 20s desde a última atividade → segue destrancado.
        assertTrue(lock.refresh(nowMs = 40_000))
    }

    @Test
    fun `lock explicito tranca na hora`() {
        val lock = AutoLockController(timeoutMs = 30_000)
        lock.unlock(nowMs = 0)
        lock.lock()
        assertFalse(lock.isUnlocked)
    }

    @Test
    fun `atividade sem estar destrancado nao destranca`() {
        val lock = AutoLockController(timeoutMs = 30_000)
        lock.onActivity(nowMs = 5_000)
        assertFalse(lock.isUnlocked)
        assertFalse(lock.refresh(nowMs = 6_000))
    }
}
