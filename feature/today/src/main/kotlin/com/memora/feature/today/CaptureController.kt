package com.memora.feature.today

import kotlinx.coroutines.flow.StateFlow

/**
 * Comando de captura visto pela tela "Hoje": ligar/desligar a gravação e observar se está ativa,
 * além de contadores da sessão atual (feedback visível: quantos trechos foram capturados vs.
 * descartados por silêncio).
 *
 * É um *seam* de device — a implementação real (Foreground Service + `AudioRecord`) fica em `:app`.
 * Um fake que só vira flags basta para exercitar a lógica do ViewModel. Princípio 4: ligar a captura
 * não depende do desbloqueio.
 */
interface CaptureController {
    /** `true` enquanto a captura está ativa. Reflete a fonte de verdade do serviço. */
    val isRecording: StateFlow<Boolean>

    /** Trechos com fala capturados na gravação atual. */
    val capturedCount: StateFlow<Int>

    /** Trechos descartados por silêncio (VAD) na gravação atual. */
    val droppedCount: StateFlow<Int>

    fun start()

    fun stop()
}
