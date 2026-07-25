package com.memora.core.speaker

import com.memora.core.common.model.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerAttributionTest {

    private val profile = VoiceProfile(embedding = floatArrayOf(1f, 0f, 0f), threshold = 0.65f, unknownMargin = 0.1f)

    private fun seg(id: String, vararg e: Float) = SegmentEmbedding(id, e)

    @Test
    fun `without a profile everything is UNKNOWN`() {
        val result = SpeakerAttribution.attribute(
            listOf(seg("s1", 1f, 0f, 0f), seg("s2", 0f, 1f, 0f)),
            profile = null,
        )
        assertEquals(listOf("s1", "s2"), result.map { it.segmentId })
        assertTrue(result.all { it.decision.label == SpeakerLabel.UNKNOWN })
        assertTrue(result.all { it.decision.similarity == 0f })
    }

    @Test
    fun `with a profile each segment is classified`() {
        val result = SpeakerAttribution.attribute(
            listOf(
                seg("self", 1f, 0f, 0f),   // igual ao perfil → SELF
                seg("other", -1f, 0f, 0f), // oposto → OTHER
            ),
            profile,
        )
        assertEquals(SpeakerLabel.SELF, result[0].decision.label)
        assertEquals(SpeakerLabel.OTHER, result[1].decision.label)
    }

    @Test
    fun `empty input yields empty output`() {
        assertTrue(SpeakerAttribution.attribute(emptyList(), profile).isEmpty())
    }

    @Test
    fun `order and ids are preserved`() {
        val result = SpeakerAttribution.attribute(listOf(seg("b", 1f, 0f, 0f), seg("a", 1f, 0f, 0f)), profile)
        assertEquals(listOf("b", "a"), result.map { it.segmentId })
    }
}
