package com.memora.core.transcription.fake

import com.memora.core.common.model.AudioChunk
import com.memora.core.transcription.TranscriptResult
import com.memora.core.transcription.TranscriptSegment
import com.memora.core.transcription.TranscriptionProvider
import com.memora.core.transcription.WhisperOptions

/**
 * Fake determinístico do [TranscriptionProvider]. Devolve segmentos programados por chunkId
 * (ou um segmento default), sem tocar em áudio real. Usado por testes de pipeline e previews.
 *
 * Reside em `main` (não em testFixtures) de propósito, para poder alimentar Previews de Compose
 * durante o desenvolvimento das features. Migrar para testFixtures se virar peso no APK final.
 */
class FakeTranscriptionProvider(
    private val scripted: Map<String, TranscriptResult> = emptyMap(),
    private val defaultText: String = "trecho transcrito de exemplo",
) : TranscriptionProvider {

    var lastOptions: WhisperOptions? = null
        private set

    override suspend fun transcribe(chunk: AudioChunk, options: WhisperOptions): TranscriptResult {
        lastOptions = options
        return scripted[chunk.id] ?: TranscriptResult(
            chunkId = chunk.id,
            language = options.language ?: "pt",
            segments = listOf(
                TranscriptSegment(
                    text = defaultText,
                    startMs = 0,
                    endMs = chunk.durationMs,
                    confidence = 0.9f,
                ),
            ),
        )
    }
}
