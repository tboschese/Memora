package com.memora.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado do desbloqueio. [isUnlocked] é o sinal para a navegação seguir para a leitura. [error] é
 * [PinError.WRONG_PIN] quando o PIN não confere. [lockedForMs] > 0 indica rate-limiting em curso
 * (muitas tentativas erradas) — a UI mostra a espera e o submit é ignorado.
 */
data class UnlockUiState(
    val isUnlocked: Boolean = false,
    val error: PinError? = null,
    val isSubmitting: Boolean = false,
    val lockedForMs: Long = 0,
)

/**
 * Desbloqueio de sessão (plano §4.1): confere o PIN pelo [PinGate] e libera a leitura. Aplica
 * [PinLockout] — após tentativas erradas demais, impõe uma espera com backoff (o PIN tem baixa
 * entropia). A captura segue em background independentemente disto (princípio 4).
 */
class UnlockViewModel(
    private val gate: PinGate,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private var failures = 0
    private var lastFailureMs = 0L

    fun submit(pin: CharArray) {
        if (_uiState.value.isSubmitting) return

        val remaining = remainingLockMs()
        if (remaining > 0) {
            _uiState.value = UnlockUiState(error = PinError.WRONG_PIN, lockedForMs = remaining)
            return
        }

        val formError = PinPolicy.validate(pin)
        if (formError != null) {
            registerFailure()
            _uiState.value = UnlockUiState(error = PinError.WRONG_PIN, lockedForMs = remainingLockMs())
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            if (gate.unlock(pin)) {
                failures = 0
                _uiState.value = UnlockUiState(isUnlocked = true)
            } else {
                registerFailure()
                _uiState.value = UnlockUiState(error = PinError.WRONG_PIN, lockedForMs = remainingLockMs())
            }
        }
    }

    private fun registerFailure() {
        failures++
        lastFailureMs = now()
    }

    /** Milissegundos restantes de espera; 0 se liberado. */
    private fun remainingLockMs(): Long {
        val delay = PinLockout.delayMsAfter(failures)
        if (delay == 0L) return 0
        return (delay - (now() - lastFailureMs)).coerceAtLeast(0)
    }
}
