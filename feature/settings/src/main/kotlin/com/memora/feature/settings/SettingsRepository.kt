package com.memora.feature.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Fonte de verdade dos [MemoraSettings]. Interface aqui; a impl real (SharedPreferences) fica em
 * `:app`. Os ajustes não são sensíveis (thresholds/horas), então não exigem cifragem. [update]
 * normaliza antes de persistir — quem lê nunca vê valor fora de faixa.
 */
interface SettingsRepository {
    val settings: StateFlow<MemoraSettings>

    fun update(settings: MemoraSettings)
}
