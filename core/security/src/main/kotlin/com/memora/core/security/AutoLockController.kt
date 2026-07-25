package com.memora.core.security

/**
 * Auto-lock por inatividade (regra 4: a senha protege a **leitura**; a captura segue em background
 * mesmo trancado). State machine pura — o tempo entra como parâmetro (`nowMs`), então é testável
 * sem relógio real. A biometria (BiometricPrompt) é só um atalho de [unlock] na camada de UI.
 */
class AutoLockController(private val timeoutMs: () -> Long) {
    /** Conveniência para um timeout fixo (usada em testes e quando não há ajuste dinâmico). */
    constructor(timeoutMs: Long) : this({ timeoutMs })

    private var unlocked = false
    private var lastActivityMs = 0L

    val isUnlocked: Boolean get() = unlocked

    /** Destranca a leitura (após PIN/biometria correta) e marca a atividade. */
    fun unlock(nowMs: Long) {
        unlocked = true
        lastActivityMs = nowMs
    }

    /** Tranca imediatamente (ex.: usuário saiu do app / botão de lock). */
    fun lock() {
        unlocked = false
    }

    /** Registra interação do usuário, adiando o auto-lock. Sem efeito se já trancado. */
    fun onActivity(nowMs: Long) {
        if (unlocked) lastActivityMs = nowMs
    }

    /**
     * Aplica o timeout: se passou de [timeoutMs] desde a última atividade, tranca. Deve ser chamado
     * ao retomar o app / em ticks. Retorna o estado resultante (`true` = destrancado).
     */
    fun refresh(nowMs: Long): Boolean {
        if (unlocked && nowMs - lastActivityMs >= timeoutMs()) unlocked = false
        return unlocked
    }
}
