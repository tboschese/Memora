package com.memora.core.digest

import com.memora.core.common.model.SpeakerLabel

/**
 * Gera o digest de um dia (impl real: LLM local via llama.cpp; futuramente também um modelo
 * remoto pago, opt-in). A saída é estruturada (validável por schema JSON), não texto livre.
 *
 * Mantido desacoplado de :core:transcription — recebe linhas genéricas em [DigestSource].
 */
interface DigestProvider {
    suspend fun generate(input: DigestInput): Digest
}

data class DigestInput(
    val epochDay: Long,
    val sources: List<DigestSource>,
    /** Grafias canônicas do glossário, injetadas no system prompt. */
    val glossaryTerms: List<String> = emptyList(),
)

data class DigestSource(
    val timeMs: Long,
    val speaker: SpeakerLabel,
    val text: String,
    val place: String? = null,
)

/** Saída estruturada do digest. Cada lista é validada por schema na impl real. */
data class Digest(
    val epochDay: Long,
    val summary: String,
    val decisions: List<String> = emptyList(),
    val myActionItems: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
)
