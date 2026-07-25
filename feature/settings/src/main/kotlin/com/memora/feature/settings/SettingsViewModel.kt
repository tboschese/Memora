package com.memora.feature.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel dos Ajustes: expõe os [MemoraSettings] atuais e persiste as edições (sempre
 * normalizadas pela camada de repositório). [reset] volta aos padrões.
 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<MemoraSettings> = repository.settings

    fun update(settings: MemoraSettings) = repository.update(settings)

    fun reset() = repository.update(MemoraSettings.DEFAULT)
}
