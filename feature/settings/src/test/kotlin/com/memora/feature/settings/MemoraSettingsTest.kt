package com.memora.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoraSettingsTest {

    @Test
    fun `defaults are already normalized`() {
        assertEquals(MemoraSettings.DEFAULT, MemoraSettings.DEFAULT.normalized())
    }

    @Test
    fun `speaker threshold is clamped to 0-1`() {
        assertEquals(1f, MemoraSettings(speakerThreshold = 5f).normalized().speakerThreshold, 0f)
        assertEquals(0f, MemoraSettings(speakerThreshold = -1f).normalized().speakerThreshold, 0f)
    }

    @Test
    fun `unknown margin cannot exceed the threshold`() {
        val s = MemoraSettings(speakerThreshold = 0.5f, speakerUnknownMargin = 0.9f).normalized()
        assertEquals(0.5f, s.speakerUnknownMargin, 0f)
    }

    @Test
    fun `negative vad threshold clamps to zero`() {
        assertEquals(0.0, MemoraSettings(vadRmsThreshold = -10.0).normalized().vadRmsThreshold, 0.0)
    }

    @Test
    fun `auto-lock has a minimum`() {
        val s = MemoraSettings(autoLockTimeoutMs = 0).normalized()
        assertEquals(MemoraSettings.MIN_AUTO_LOCK_MS, s.autoLockTimeoutMs)
    }

    @Test
    fun `digest hour is clamped to 0-23`() {
        assertEquals(23, MemoraSettings(digestTargetHour = 99).normalized().digestTargetHour)
        assertEquals(0, MemoraSettings(digestTargetHour = -5).normalized().digestTargetHour)
    }

    @Test
    fun `language is trimmed lowercased and blanks become null (auto)`() {
        assertEquals("pt", MemoraSettings(transcriptionLanguage = "  PT ").normalized().transcriptionLanguage)
        assertNull(MemoraSettings(transcriptionLanguage = "   ").normalized().transcriptionLanguage)
        assertNull(MemoraSettings(transcriptionLanguage = null).normalized().transcriptionLanguage)
    }

    @Test
    fun `normalized is idempotent`() {
        val messy = MemoraSettings(
            transcriptionLanguage = " EN ",
            speakerThreshold = 2f,
            speakerUnknownMargin = 9f,
            vadRmsThreshold = -1.0,
            autoLockTimeoutMs = -5,
            digestTargetHour = 40,
        )
        val once = messy.normalized()
        assertEquals(once, once.normalized())
    }

    @Test
    fun `reset returns the defaults`() {
        assertTrue(MemoraSettings.DEFAULT === MemoraSettings.DEFAULT) // sanity: DEFAULT existe
        assertEquals(MemoraSettings(), MemoraSettings.DEFAULT)
    }
}
