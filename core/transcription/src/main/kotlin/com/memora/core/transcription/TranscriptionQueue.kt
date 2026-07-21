package com.memora.core.transcription

import com.memora.core.common.log.SafeLog
import com.memora.core.common.model.AudioChunk
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fila de transcrição (Fase 1, §4.3 do plano). Orquestra o pipeline
 * `chunk → transcreve → persiste texto → destrói áudio`, com duas políticas invioláveis:
 *
 * - **Áudio efêmero (regra 2):** o áudio de um chunk só é destruído DEPOIS que o texto foi
 *   persistido com sucesso. Se a persistência falhar, o áudio permanece na fila para retry —
 *   nunca destruímos antes de ter o texto salvo.
 * - **Nada se perde silenciosamente:** quando algo é descartado (fila cheia, áudio ausente,
 *   falha repetida de transcrição), registramos um [TranscriptionGap] na timeline em vez de
 *   sumir com o intervalo sem deixar rastro.
 *
 * Política contínuo vs. lote (RF-10/RF-11): [DrainMode.CONTINUOUS] drena assim que há trabalho;
 * [DrainMode.BATCH] só drena quando o device está carregando ou ocioso — para concentrar o
 * custo de compute quando barato (à noite / no carregador).
 *
 * Descarte por limite de fila (RNF-02, 200 MB por padrão): se a captura supera a velocidade de
 * transcrição e o áudio pendente ultrapassa o limite, os chunks MAIS ANTIGOS são descartados
 * (áudio destruído sem transcrever) e um gap `QUEUE_OVERFLOW` cobre o intervalo perdido.
 *
 * A classe é pura orquestração: não conhece `:core:audio` nem `:core:db`. Os colaboradores
 * ([AudioChunkAccess], [SegmentSink], [GapSink]) são interfaces pequenas, o que a torna
 * inteiramente testável em JVM com fakes — sem device, modelo ou Room.
 */
class TranscriptionQueue(
    private val audio: AudioChunkAccess,
    private val provider: TranscriptionProvider,
    private val segments: SegmentSink,
    private val gaps: GapSink,
    private val options: WhisperOptions = WhisperOptions(),
    /** Limite de áudio pendente em disco/memória antes de descartar os mais antigos. */
    private val maxQueueBytes: Long = DEFAULT_MAX_QUEUE_BYTES,
    /** Tentativas de transcrição por chunk antes de registrar gap e desistir. */
    private val maxAttempts: Int = 3,
) {
    /** Fila FIFO de chunks pendentes (o mais antigo primeiro). Protegida por [mutex]. */
    private val pending = ArrayDeque<PendingChunk>()
    private val attempts = HashMap<String, Int>()
    private val mutex = Mutex()

    /** Áudio total ainda não transcrito. Exposto para métricas/testes. */
    suspend fun pendingBytes(): Long = mutex.withLock { pending.sumOf { it.sizeBytes } }

    suspend fun pendingCount(): Int = mutex.withLock { pending.size }

    /**
     * Registra um chunk recém-capturado (o áudio já está no [AudioChunkAccess]). Aplica o
     * limite de fila imediatamente: se estourar [maxQueueBytes], descarta os mais antigos com gap.
     */
    suspend fun enqueue(chunk: PendingChunk) {
        val evicted = mutex.withLock {
            pending.addLast(chunk)
            attempts[chunk.chunkId] = 0
            evictOverflowLocked()
        }
        emitOverflowGap(evicted)
    }

    /**
     * Processa a fila conforme a política. Em [DrainMode.CONTINUOUS] drena tudo que puder; em
     * [DrainMode.BATCH] só drena se [device] estiver carregando ou ocioso. Retorna quantos chunks
     * foram transcritos com sucesso nesta passagem.
     */
    suspend fun drain(mode: DrainMode, device: DeviceState): Int {
        if (!mode.shouldDrain(device)) return 0
        var transcribed = 0
        while (true) {
            val next = mutex.withLock { pending.firstOrNull() } ?: break
            if (processOne(next)) transcribed++
        }
        return transcribed
    }

    /**
     * Processa um chunk ponta a ponta. Retorna `true` se o texto foi persistido (chunk sai da
     * fila e áudio é destruído). Em falha, decide entre retry e gap sem remover indevidamente.
     */
    private suspend fun processOne(chunk: PendingChunk): Boolean {
        val loaded: AudioChunk? = audio.load(chunk.chunkId)
        if (loaded == null) {
            // Áudio sumiu (crash/limpeza externa) antes de transcrever: registra gap e segue.
            removeLocked(chunk.chunkId)
            gaps.recordGap(chunk.toGap(GapReason.AUDIO_MISSING))
            SafeLog.w("transcribe_audio_missing", SafeLog.Id("chunkId", chunk.chunkId))
            return false
        }

        val result = try {
            provider.transcribe(loaded, options)
        } catch (t: Throwable) {
            return onTranscribeFailure(chunk, t)
        }

        // Texto primeiro: só destrói o áudio depois de persistir (regra 2).
        segments.persist(result)
        audio.destroy(chunk.chunkId)
        removeLocked(chunk.chunkId)
        SafeLog.i(
            "transcribe_ok",
            SafeLog.Id("chunkId", chunk.chunkId),
            SafeLog.Count("segments", result.segments.size.toLong()),
        )
        return true
    }

    /** Conta a tentativa; após [maxAttempts] falhas, registra gap e descarta o áudio. */
    private suspend fun onTranscribeFailure(chunk: PendingChunk, cause: Throwable): Boolean {
        val count = mutex.withLock {
            val c = (attempts[chunk.chunkId] ?: 0) + 1
            attempts[chunk.chunkId] = c
            c
        }
        if (count >= maxAttempts) {
            audio.destroy(chunk.chunkId)
            removeLocked(chunk.chunkId)
            gaps.recordGap(chunk.toGap(GapReason.TRANSCRIBE_FAILED))
            SafeLog.e(
                "transcribe_gave_up",
                SafeLog.Id("chunkId", chunk.chunkId),
                SafeLog.Count("attempts", count.toLong()),
            )
            return false
        }
        // Reenfileira ao fim para não travar a fila num único chunk problemático.
        mutex.withLock {
            pending.remove(chunk)
            pending.addLast(chunk)
        }
        SafeLog.w(
            "transcribe_retry",
            SafeLog.Id("chunkId", chunk.chunkId),
            SafeLog.Count("attempt", count.toLong()),
        )
        return false
    }

    private suspend fun removeLocked(chunkId: String) = mutex.withLock {
        pending.removeAll { it.chunkId == chunkId }
        attempts.remove(chunkId)
    }

    /** DEVE ser chamado com [mutex] já travado. Remove os mais antigos até caber em [maxQueueBytes]. */
    private fun evictOverflowLocked(): List<PendingChunk> {
        if (pending.sumOf { it.sizeBytes } <= maxQueueBytes) return emptyList()
        val evicted = ArrayList<PendingChunk>()
        var total = pending.sumOf { it.sizeBytes }
        // Nunca esvazia a fila inteira: mantém ao menos o chunk mais novo.
        while (total > maxQueueBytes && pending.size > 1) {
            val oldest = pending.removeFirst()
            attempts.remove(oldest.chunkId)
            total -= oldest.sizeBytes
            evicted.add(oldest)
        }
        return evicted
    }

    /** Destrói o áudio dos chunks descartados e registra um único gap cobrindo o intervalo. */
    private suspend fun emitOverflowGap(evicted: List<PendingChunk>) {
        if (evicted.isEmpty()) return
        for (c in evicted) audio.destroy(c.chunkId)
        val from = evicted.minOf { it.startedAtMs }
        val to = evicted.maxOf { it.startedAtMs + it.durationMs }
        gaps.recordGap(TranscriptionGap(from, to, GapReason.QUEUE_OVERFLOW))
        SafeLog.w(
            "queue_overflow_drop",
            SafeLog.Count("chunks", evicted.size.toLong()),
            SafeLog.Count("bytes", evicted.sumOf { it.sizeBytes }),
        )
    }

    companion object {
        /** 200 MB — RNF-02. */
        const val DEFAULT_MAX_QUEUE_BYTES: Long = 200L * 1024 * 1024
    }
}

/** Metadados de um chunk aguardando transcrição (o áudio vive no [AudioChunkAccess]). */
data class PendingChunk(
    val chunkId: String,
    val startedAtMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
) {
    fun toGap(reason: GapReason) = TranscriptionGap(startedAtMs, startedAtMs + durationMs, reason)
}

/** Quando drenar a fila. */
enum class DrainMode {
    /** Transcreve assim que há trabalho (RF-10). */
    CONTINUOUS,

    /** Só transcreve com o device carregando ou ocioso (RF-11) — compute quando barato. */
    BATCH,
    ;

    fun shouldDrain(device: DeviceState): Boolean = when (this) {
        CONTINUOUS -> true
        BATCH -> device.charging || device.idle
    }
}

/** Estado do device que governa a política [DrainMode.BATCH]. */
data class DeviceState(val charging: Boolean, val idle: Boolean)

/** Intervalo perdido na timeline, com o motivo. Nunca sumimos com áudio sem registrar isto. */
data class TranscriptionGap(val fromMs: Long, val toMs: Long, val reason: GapReason)

enum class GapReason {
    /** Fila estourou o limite de bytes; chunks mais antigos descartados. */
    QUEUE_OVERFLOW,

    /** Áudio não estava mais disponível ao transcrever (crash/limpeza). */
    AUDIO_MISSING,

    /** Transcrição falhou além do limite de tentativas. */
    TRANSCRIBE_FAILED,
}

/**
 * Acesso ao áudio efêmero, na visão da fila. Implementado sobre `EphemeralAudioStore`
 * (:core:audio) sem que este módulo dependa daquele.
 */
interface AudioChunkAccess {
    /** Carrega o PCM do chunk, ou `null` se já não existe. */
    suspend fun load(chunkId: String): AudioChunk?

    /** Destrói o áudio do chunk de forma irreversível (idempotente). */
    suspend fun destroy(chunkId: String)
}

/** Persiste o texto transcrito de forma durável ANTES de o áudio ser destruído. */
fun interface SegmentSink {
    suspend fun persist(result: TranscriptResult)
}

/** Registra um gap na timeline. */
fun interface GapSink {
    suspend fun recordGap(gap: TranscriptionGap)
}
