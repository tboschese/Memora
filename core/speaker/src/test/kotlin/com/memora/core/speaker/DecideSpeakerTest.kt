package com.memora.core.speaker

import com.memora.core.common.model.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class DecideSpeakerTest {

    private val profileVec = floatArrayOf(1f, 0f, 0f)
    private val profile = VoiceProfile(embedding = profileVec, threshold = 0.65f, unknownMargin = 0.1f)

    @Test
    fun `voz identica ao perfil e SELF`() {
        val d = decideSpeaker(floatArrayOf(1f, 0f, 0f), profile)
        assertEquals(SpeakerLabel.SELF, d.label)
    }

    @Test
    fun `voz ortogonal ao perfil e OTHER`() {
        val d = decideSpeaker(floatArrayOf(0f, 1f, 0f), profile)
        assertEquals(SpeakerLabel.OTHER, d.label)
    }

    @Test
    fun `similaridade na margem vira UNKNOWN, nunca chute`() {
        // Vetor com cosseno ~0.6 contra (1,0,0): fica entre (threshold - margin)=0.55 e threshold=0.65.
        val v = floatArrayOf(0.6f, 0.8f, 0f) // cos = 0.6
        val d = decideSpeaker(v, profile)
        assertEquals(SpeakerLabel.UNKNOWN, d.label)
    }
}
