package com.memora.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinLockoutTest {

    @Test
    fun `no delay within the free attempts`() {
        (0..PinLockout.FREE_ATTEMPTS).forEach { assertEquals(0L, PinLockout.delayMsAfter(it)) }
    }

    @Test
    fun `backoff doubles after the free attempts`() {
        assertEquals(PinLockout.BASE_DELAY_MS, PinLockout.delayMsAfter(PinLockout.FREE_ATTEMPTS + 1))
        assertEquals(PinLockout.BASE_DELAY_MS * 2, PinLockout.delayMsAfter(PinLockout.FREE_ATTEMPTS + 2))
        assertEquals(PinLockout.BASE_DELAY_MS * 4, PinLockout.delayMsAfter(PinLockout.FREE_ATTEMPTS + 3))
    }

    @Test
    fun `delay is capped and never overflows`() {
        assertEquals(PinLockout.MAX_DELAY_MS, PinLockout.delayMsAfter(100))
        assertTrue(PinLockout.delayMsAfter(Int.MAX_VALUE) in 1..PinLockout.MAX_DELAY_MS)
    }
}
