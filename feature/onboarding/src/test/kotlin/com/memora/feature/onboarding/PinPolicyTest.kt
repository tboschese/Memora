package com.memora.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {

    @Test
    fun `accepts a numeric pin within bounds`() {
        assertNull(PinPolicy.validate("1234".toCharArray()))
        assertNull(PinPolicy.validate("12345678".toCharArray()))
        assertTrue(PinPolicy.isValid("4321".toCharArray()))
    }

    @Test
    fun `rejects too short`() {
        assertEquals(PinError.TOO_SHORT, PinPolicy.validate("123".toCharArray()))
        assertEquals(PinError.TOO_SHORT, PinPolicy.validate(CharArray(0)))
    }

    @Test
    fun `rejects too long`() {
        assertEquals(PinError.TOO_LONG, PinPolicy.validate("123456789".toCharArray()))
    }

    @Test
    fun `rejects non-numeric`() {
        assertEquals(PinError.NOT_NUMERIC, PinPolicy.validate("12a4".toCharArray()))
        assertEquals(PinError.NOT_NUMERIC, PinPolicy.validate("12 4".toCharArray()))
    }

    @Test
    fun `length is checked before content`() {
        // "12a" é curto E não-numérico: o erro de forma mais básico (tamanho) vem primeiro.
        assertEquals(PinError.TOO_SHORT, PinPolicy.validate("12a".toCharArray()))
    }
}
