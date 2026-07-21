package com.memora.app.data

import com.memora.core.audio.EphemeralAudioStore
import com.memora.core.common.model.AudioChunk
import com.memora.core.transcription.AudioChunkAccess
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Liga o [EphemeralAudioStore] (:core:audio, que guarda bytes) à fila de transcrição
 * (:core:transcription, que pede um `AudioChunk`). O `CapturePipeline` grava os bytes e
 * [register] os metadados; a fila chama [load] para reconstruir o `AudioChunk` antes de
 * transcrever, e [destroy] para apagar o áudio depois de o texto persistir.
 *
 * Vive no :app (raiz de composição) para não acoplar :core:audio a :core:transcription.
 */
class EphemeralAudioChunkAccess(
    private val store: EphemeralAudioStore,
    private val sampleRate: Int,
) : AudioChunkAccess {

    private data class Meta(val startedAtMs: Long, val durationMs: Long)

    private val meta = ConcurrentHashMap<String, Meta>()

    /** Registra os metadados de um chunk recém-gravado (do `CapturePipeline.StoredChunk`). */
    fun register(chunkId: String, startedAtMs: Long, durationMs: Long) {
        meta[chunkId] = Meta(startedAtMs, durationMs)
    }

    override suspend fun load(chunkId: String): AudioChunk? {
        val bytes = store.read(chunkId) ?: return null
        val m = meta[chunkId] ?: return null
        return AudioChunk(
            id = chunkId,
            pcm16 = bytes.toLittleEndianShorts(),
            sampleRate = sampleRate,
            startedAt = Instant.ofEpochMilli(m.startedAtMs),
            durationMs = m.durationMs,
        )
    }

    override suspend fun destroy(chunkId: String) {
        store.destroy(chunkId)
        meta.remove(chunkId)
    }
}

/** Desserializa bytes little-endian de volta para PCM 16-bit. */
internal fun ByteArray.toLittleEndianShorts(): ShortArray {
    val out = ShortArray(size / 2)
    for (i in out.indices) {
        val lo = this[i * 2].toInt() and 0xFF
        val hi = this[i * 2 + 1].toInt() and 0xFF
        out[i] = ((hi shl 8) or lo).toShort()
    }
    return out
}
