package com.memora.core.speaker

import com.memora.core.common.model.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class VoiceEnrollmentTest {

    private fun norm(v: FloatArray): Float {
        var n = 0f
        for (x in v) n += x * x
        return sqrt(n)
    }

    @Test
    fun `profile is the normalized centroid of the samples`() {
        val samples = listOf(
            floatArrayOf(2f, 0f, 0f),
            floatArrayOf(0f, 2f, 0f),
        )
        val result = VoiceEnrollment.buildProfile(samples)

        // centroide (1,1,0) normalizado ~ (0.707, 0.707, 0)
        val e = result.profile.embedding
        assertEquals(0.7071f, e[0], 1e-4f)
        assertEquals(0.7071f, e[1], 1e-4f)
        assertEquals(0f, e[2], 1e-4f)
        assertEquals(1f, norm(e), 1e-5f) // unitário
    }

    @Test
    fun `identical samples give perfect cohesion`() {
        val v = floatArrayOf(1f, 2f, 3f)
        val result = VoiceEnrollment.buildProfile(listOf(v.copyOf(), v.copyOf(), v.copyOf()))
        assertEquals(1f, result.cohesion, 1e-5f)
    }

    @Test
    fun `spread samples give lower cohesion than tight ones`() {
        val tight = VoiceEnrollment.buildProfile(
            listOf(floatArrayOf(1f, 0.1f, 0f), floatArrayOf(1f, 0f, 0.1f)),
        ).cohesion
        val spread = VoiceEnrollment.buildProfile(
            listOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f)),
        ).cohesion
        assertTrue("tight=$tight spread=$spread", tight > spread)
    }

    @Test
    fun `the resulting profile classifies a nearby embedding as SELF`() {
        val result = VoiceEnrollment.buildProfile(
            listOf(floatArrayOf(1f, 0.2f, 0f), floatArrayOf(1f, 0f, 0.2f)),
        )
        val decision = decideSpeaker(floatArrayOf(1f, 0.1f, 0.1f), result.profile)
        assertEquals(SpeakerLabel.SELF, decision.label)
    }

    @Test
    fun `too few samples is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            VoiceEnrollment.buildProfile(listOf(floatArrayOf(1f, 2f)))
        }
    }

    @Test
    fun `mismatched dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            VoiceEnrollment.buildProfile(listOf(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f, 3f)))
        }
    }

    @Test
    fun `custom threshold and margin are carried into the profile`() {
        val result = VoiceEnrollment.buildProfile(
            listOf(floatArrayOf(1f, 0f), floatArrayOf(1f, 0f)),
            threshold = 0.8f,
            unknownMargin = 0.05f,
        )
        assertEquals(0.8f, result.profile.threshold, 0f)
        assertEquals(0.05f, result.profile.unknownMargin, 0f)
    }
}
