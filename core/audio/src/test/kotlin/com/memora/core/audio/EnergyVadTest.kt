package com.memora.core.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyVadTest {

    private val vad = EnergyVad(rmsThreshold = 500.0)

    @Test
    fun `silencio nao tem fala`() {
        assertFalse(vad.hasSpeech(ShortArray(1000))) // tudo zero
    }

    @Test
    fun `trecho vazio nao tem fala`() {
        assertFalse(vad.hasSpeech(ShortArray(0)))
    }

    @Test
    fun `sinal acima do limiar conta como fala`() {
        // amplitude constante 1000 → RMS = 1000 >= 500
        assertTrue(vad.hasSpeech(ShortArray(1000) { 1000 }))
    }

    @Test
    fun `ruido fraco abaixo do limiar nao conta como fala`() {
        // amplitude constante 100 → RMS = 100 < 500
        assertFalse(vad.hasSpeech(ShortArray(1000) { 100 }))
    }
}
