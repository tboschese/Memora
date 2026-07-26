package com.memora.app.capture

import com.memora.core.common.model.AudioChunk
import com.memora.core.transcription.TranscriptResult
import com.memora.core.transcription.TranscriptSegment
import com.memora.core.transcription.TranscriptionProvider
import com.memora.core.transcription.WhisperOptions

/**
 * Transcritor **placeholder** até o whisper.cpp (JNI) existir. Não inventa texto: marca honestamente
 * que houve fala capturada, com a duração, para que a captura ponta-a-ponta seja visível na timeline
 * sem poluir o diário com conteúdo falso. É o *seam* onde o whisper real entra sem tocar no resto.
 */
class PlaceholderTranscriptionProvider : TranscriptionProvider {
    override suspend fun transcribe(chunk: AudioChunk, options: WhisperOptions): TranscriptResult {
        val seconds = (chunk.durationMs / 1000.0)
        return TranscriptResult(
            chunkId = chunk.id,
            language = options.language ?: "pt",
            segments = listOf(
                TranscriptSegment(
                    text = "🎙️ fala capturada (${"%.0f".format(seconds)}s) — transcrição on-device em breve",
                    startMs = 0,
                    endMs = chunk.durationMs,
                    confidence = 1f,
                ),
            ),
        )
    }
}
