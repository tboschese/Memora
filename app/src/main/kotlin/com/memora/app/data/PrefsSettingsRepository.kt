package com.memora.app.data

import android.content.Context
import com.memora.feature.settings.DarkMode
import com.memora.feature.settings.MemoraSettings
import com.memora.feature.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persiste os [MemoraSettings] em `SharedPreferences` (não sensível — thresholds/horas). Guarda
 * apenas os campos que a UI edita hoje; o resto usa o default. Expõe um [StateFlow] para a tela
 * reagir; [update] normaliza, grava e emite.
 */
@Singleton
class PrefsSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<MemoraSettings> = _settings.asStateFlow()

    override fun update(settings: MemoraSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putLong(KEY_AUTO_LOCK, normalized.autoLockTimeoutMs)
            .putInt(KEY_DIGEST_HOUR, normalized.digestTargetHour)
            .putInt(KEY_DARK_MODE, normalized.darkMode.ordinal)
            .apply()
        _settings.value = normalized
    }

    private fun load(): MemoraSettings {
        val defaults = MemoraSettings.DEFAULT
        return MemoraSettings(
            autoLockTimeoutMs = prefs.getLong(KEY_AUTO_LOCK, defaults.autoLockTimeoutMs),
            digestTargetHour = prefs.getInt(KEY_DIGEST_HOUR, defaults.digestTargetHour),
            darkMode = DarkMode.entries.getOrElse(prefs.getInt(KEY_DARK_MODE, defaults.darkMode.ordinal)) { DarkMode.SYSTEM },
        ).normalized()
    }

    private companion object {
        const val PREFS = "memora_settings"
        const val KEY_AUTO_LOCK = "auto_lock_ms"
        const val KEY_DIGEST_HOUR = "digest_hour"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
