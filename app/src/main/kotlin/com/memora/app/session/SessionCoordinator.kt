package com.memora.app.session

import com.memora.core.security.AutoLockController
import com.memora.feature.onboarding.PinGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fase da sessão que decide a navegação do app: definir o PIN, desbloquear, ou já ler.
 * - [ONBOARDING]: ainda não há PIN configurado.
 * - [LOCKED]: há PIN, mas a leitura está trancada (auto-lock ou primeira abertura).
 * - [UNLOCKED]: leitura liberada.
 *
 * Regra 4: isto governa só a **leitura**. A captura roda em background independentemente da fase.
 */
enum class SessionPhase { ONBOARDING, LOCKED, UNLOCKED }

/**
 * Cérebro de navegação do app: combina "existe PIN?" ([PinGate.isConfigured]) com o estado do
 * [AutoLockController] e expõe a [SessionPhase] atual como um `StateFlow` que a UI observa.
 *
 * É reativo por cima de duas peças imperativas: o gate (que, ao autenticar, já destranca o
 * auto-lock) e o controlador de auto-lock (cujo tempo entra por parâmetro). Por isso os eventos
 * externos — autenticou, houve atividade, tick de timeout — chegam por métodos explícitos, e cada
 * um recomputa a fase. Máquina de estados pura no miolo: testável sem relógio nem device.
 */
class SessionCoordinator(
    private val gate: PinGate,
    private val autoLock: AutoLockController,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _phase = MutableStateFlow(compute())
    val phase: StateFlow<SessionPhase> = _phase.asStateFlow()

    /** Chamar após um setup/unlock bem-sucedido no gate (que já destrancou o auto-lock). */
    fun onAuthenticated() = emit()

    /** Registra interação do usuário para adiar o auto-lock (regra 4). */
    fun onActivity() = autoLock.onActivity(clock())

    /** Aplica o timeout de auto-lock e recomputa — chamar ao retomar o app / em ticks. */
    fun refresh() {
        autoLock.refresh(clock())
        emit()
    }

    /** Tranca a leitura manualmente (ex.: usuário saiu do app / botão de lock). */
    fun lock() {
        autoLock.lock()
        emit()
    }

    private fun emit() {
        _phase.value = compute()
    }

    private fun compute(): SessionPhase = when {
        !gate.isConfigured() -> SessionPhase.ONBOARDING
        autoLock.isUnlocked -> SessionPhase.UNLOCKED
        else -> SessionPhase.LOCKED
    }
}
