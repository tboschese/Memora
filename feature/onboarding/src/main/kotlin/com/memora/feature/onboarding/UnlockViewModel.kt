package com.memora.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado do desbloqueio. [isUnlocked] é o sinal para a navegação seguir para a leitura. [error] é
 * [PinError.WRONG_PIN] quando o PIN não confere.
 */
data class UnlockUiState(
    val isUnlocked: Boolean = false,
    val error: PinError? = null,
    val isSubmitting: Boolean = false,
)

/**
 * Desbloqueio de sessão (plano §4.1): confere o PIN pelo [PinGate] e libera a leitura. A captura
 * segue em background independentemente disto (princípio 4). PIN de forma inválida nem chega ao
 * gate — [PinPolicy] barra antes, poupando o PBKDF2.
 */
class UnlockViewModel(private val gate: PinGate) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    fun submit(pin: CharArray) {
        if (_uiState.value.isSubmitting) return
        val formError = PinPolicy.validate(pin)
        if (formError != null) {
            _uiState.value = _uiState.value.copy(error = PinError.WRONG_PIN)
            return
        }
        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            val ok = gate.unlock(pin)
            _uiState.value = if (ok) {
                UnlockUiState(isUnlocked = true)
            } else {
                UnlockUiState(error = PinError.WRONG_PIN)
            }
        }
    }
}
