package com.memora.core.transcription

import com.memora.core.common.model.AudioChunk

/**
 * Transcreve áudio localmente (impl real: whisper.cpp via JNI). Atrás de interface para permitir
 * fake determinístico em testes e desenvolvimento das features sem device/modelo.
 */
interface TranscriptionProvider {
    suspend fun transcribe(chunk: AudioChunk, options: WhisperOptions): TranscriptResult
}

/**
 * Opções do Whisper. `initialPrompt` é onde o glossário é injetado (grafias canônicas) —
 * respeitar o limite de ~224 tokens do Whisper na impl real.
 */
data class WhisperOptions(
    val language: String? = null, // null = auto-detect
    val initialPrompt: String? = null,
    val translate: Boolean = false,
)

data class TranscriptResult(
    val chunkId: String,
    val language: String,
    val segments: List<TranscriptSegment>,
)

data class TranscriptSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float, // 0f..1f
)
