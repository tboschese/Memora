package com.memora.feature.today

import kotlinx.coroutines.flow.StateFlow

/**
 * Comando de captura visto pela tela "Hoje": ligar/desligar a gravação e observar se está ativa.
 *
 * É um *seam* de device — a implementação real (Foreground Service + `AudioRecord`) é trabalho de
 * device da Fase 1 e não roda em teste. Um fake que só vira um flag basta para exercitar toda a
 * lógica do ViewModel. Princípio 4 (captura ≠ leitura): ligar a captura não depende do desbloqueio.
 */
interface CaptureController {
    /** `true` enquanto a captura está ativa. Reflete a fonte de verdade do serviço. */
    val isRecording: StateFlow<Boolean>

    /** Inicia a captura (idempotente se já ativa). */
    fun start()

    /** Encerra a captura (idempotente se já inativa). */
    fun stop()
}
