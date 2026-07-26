package com.memora.app.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.memora.feature.settings.DarkMode
import com.memora.feature.settings.MemoraSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Round-trip dos ajustes em SharedPreferences (Robolectric), incluindo normalização e persistência. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PrefsSettingsRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `defaults when nothing was saved`() {
        val repo = PrefsSettingsRepository(context)
        assertEquals(MemoraSettings.DEFAULT.autoLockTimeoutMs, repo.settings.value.autoLockTimeoutMs)
        assertEquals(MemoraSettings.DEFAULT.digestTargetHour, repo.settings.value.digestTargetHour)
    }

    @Test
    fun `update persists and survives a new instance`() {
        PrefsSettingsRepository(context).update(
            MemoraSettings(autoLockTimeoutMs = 300_000L, digestTargetHour = 20, darkMode = DarkMode.DARK),
        )

        val reloaded = PrefsSettingsRepository(context)
        assertEquals(300_000L, reloaded.settings.value.autoLockTimeoutMs)
        assertEquals(20, reloaded.settings.value.digestTargetHour)
        assertEquals(DarkMode.DARK, reloaded.settings.value.darkMode)
    }

    @Test
    fun `update normalizes out-of-range values before saving`() {
        val repo = PrefsSettingsRepository(context)
        repo.update(MemoraSettings(autoLockTimeoutMs = 0L, digestTargetHour = 99))

        assertEquals(MemoraSettings.MIN_AUTO_LOCK_MS, repo.settings.value.autoLockTimeoutMs)
        assertEquals(23, repo.settings.value.digestTargetHour)
    }
}
