package com.memora.core.models

/**
 * Gestão dos modelos sideloaded (.gguf/.onnx). Verifica presença e integridade (checksum).
 * Features degradam graciosamente quando um modelo requerido está ausente.
 */
interface ModelRegistry {
    /** Estado atual conhecido (rápido; não recalcula checksum). */
    fun statuses(): List<ModelStatus>

    /** Recalcula presença + checksum de todos os modelos. Pode ser custoso. */
    suspend fun verify(): List<ModelStatus>
}

enum class ModelKind {
    VAD,               // Silero VAD (ONNX)
    SPEAKER_EMBEDDING, // ECAPA-TDNN (ONNX)
    TRANSCRIPTION,     // Whisper (GGUF/bin)
    DIGEST_LLM,        // Qwen/Gemma (GGUF)
}

data class ModelSpec(
    val kind: ModelKind,
    val fileName: String,
    val sha256: String,
)

data class ModelStatus(
    val spec: ModelSpec,
    val present: Boolean,
    /** null = ainda não verificado; true/false = resultado do último [ModelRegistry.verify]. */
    val checksumOk: Boolean?,
)
