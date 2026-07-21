package com.memora.core.audio

/**
 * Orquestra a captura (§4.2): `amostras PCM → chunking → VAD → store efêmero → callback`.
 *
 * Recebe blocos de PCM via [onAudio], fatia em chunks ([PcmChunker]), descarta os silenciosos
 * ([VoiceActivityDetector]) e grava os com fala no [EphemeralAudioStore]. Para cada chunk gravado,
 * chama [onStored] — que a camada de composição liga ao `enqueue` da fila de transcrição (este
 * módulo não depende de `:core:transcription`; o acoplamento fica no `:app`).
 *
 * Os tempos de início de cada chunk são derivados do total de amostras processadas (inclusive as
 * descartadas), então a timeline fica correta mesmo com silêncio no meio. Determinístico e testável
 * com fakes — sem AudioRecord real.
 */
class CapturePipeline(
    private val chunker: PcmChunker,
    private val vad: VoiceActivityDetector,
    private val store: EphemeralAudioStore,
    private val captureStartMs: Long,
    private val newChunkId: () -> String,
    private val onStored: suspend (StoredChunk) -> Unit,
) {
    /** Metadados de um chunk gravado, prontos para virar `PendingChunk` na fila. */
    data class StoredChunk(
        val chunkId: String,
        val startedAtMs: Long,
        val durationMs: Long,
        val sizeBytes: Int,
    )

    /** Quantos chunks foram descartados por silêncio (métrica, não vaza conteúdo). */
    var droppedSilenceCount = 0
        private set

    private var processedSamples = 0L

    /** Alimenta um bloco de PCM capturado. Emite os chunks completados. */
    suspend fun onAudio(samples: ShortArray) {
        for (chunk in chunker.feed(samples)) processChunk(chunk)
    }

    /** Encerra a captura: emite o chunk parcial que restou no buffer, se houver. */
    suspend fun stop() {
        chunker.flush()?.let { processChunk(it) }
    }

    private suspend fun processChunk(pcm: ShortArray) {
        val startMs = captureStartMs + processedSamples * 1000L / chunker.sampleRate
        processedSamples += pcm.size

        if (!vad.hasSpeech(pcm)) {
            droppedSilenceCount++
            return
        }
        val id = newChunkId()
        val bytes = pcm.toLittleEndianBytes()
        store.write(id, bytes)
        onStored(StoredChunk(id, startMs, chunker.durationMsOf(pcm.size), bytes.size))
    }
}

/** Serializa PCM 16-bit para bytes little-endian (formato esperado por Whisper/arquivo). */
internal fun ShortArray.toLittleEndianBytes(): ByteArray {
    val out = ByteArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt()
        out[i * 2] = (v and 0xFF).toByte()
        out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
    }
    return out
}
