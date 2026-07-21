package com.memora.core.common.model

/**
 * Atribuição de fala. NUNCA chutar: baixa confiança ⇒ [UNKNOWN].
 * - [SELF]: voz do dono do device (perfil de enrollment).
 * - [OTHER]: outra pessoa, com confiança suficiente.
 * - [UNKNOWN]: incerto — não atribuir.
 */
enum class SpeakerLabel { SELF, OTHER, UNKNOWN }

/** Resultado da classificação: rótulo + similaridade (cosseno) que o gerou. */
data class SpeakerDecision(
    val label: SpeakerLabel,
    val similarity: Float,
)
