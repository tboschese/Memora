package com.memora.app.data

import com.memora.feature.settings.MemoraSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsBindingsTest {

    @Test
    fun `whisper options carry the language and glossary prompt`() {
        val options = MemoraSettings(transcriptionLanguage = "pt").toWhisperOptions(initialPrompt = "Memora, Kubernetes")
        assertEquals("pt", options.language)
        assertEquals("Memora, Kubernetes", options.initialPrompt)
    }

    @Test
    fun `blank language becomes auto-detect (null)`() {
        assertNull(MemoraSettings(transcriptionLanguage = "  ").toWhisperOptions().language)
    }

    @Test
    fun `voice profile applies the settings thresholds`() {
        val embedding = floatArrayOf(1f, 0f)
        val profile = MemoraSettings(speakerThreshold = 0.7f, speakerUnknownMargin = 0.05f).toVoiceProfile(embedding)
        assertEquals(0.7f, profile.threshold, 0f)
        assertEquals(0.05f, profile.unknownMargin, 0f)
    }

    @Test
    fun `out-of-range settings are normalized before binding`() {
        // threshold 5f e margem 9f são inválidos: normalizados para 1f e (<=1f).
        val profile = MemoraSettings(speakerThreshold = 5f, speakerUnknownMargin = 9f).toVoiceProfile(floatArrayOf(1f))
        assertEquals(1f, profile.threshold, 0f)
        assertEquals(1f, profile.unknownMargin, 0f)
    }
}
