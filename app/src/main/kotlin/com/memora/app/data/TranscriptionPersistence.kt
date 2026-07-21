package com.memora.app.data

import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.dao.TimelineGapDao
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.TimelineGapEntity
import com.memora.core.transcription.GapSink
import com.memora.core.transcription.SegmentSink
import com.memora.core.transcription.TranscriptResult
import com.memora.core.transcription.TranscriptionGap

/**
 * Adaptadores que ligam a fila de transcrição (:core:transcription) ao banco (:core:db).
 *
 * Ficam aqui, na raiz de composição (:app), de propósito: assim `:core:db` NÃO precisa depender
 * de `:core:transcription` (o layering de infra permanece embaixo). A fila conhece apenas as
 * interfaces `SegmentSink`/`GapSink`; esta camada as implementa sobre os DAOs do Room.
 */

/**
 * Converte o resultado de um chunk transcrito em linhas persistíveis. Função pura e determinística
 * (id derivado de `chunkId:index`, sem relógio nem aleatoriedade), o que a torna trivialmente
 * testável. O speaker começa como `UNKNOWN` — a atribuição real vem depois (Fase 2), nunca chutar.
 */
fun TranscriptResult.toSegmentEntities(sessionId: String? = null): List<SegmentEntity> =
    segments.mapIndexed { index, segment ->
        SegmentEntity(
            id = "$chunkId:$index",
            sessionId = sessionId,
            chunkId = chunkId,
            text = segment.text,
            startMs = segment.startMs,
            endMs = segment.endMs,
            confidence = segment.confidence,
            language = language,
        )
    }

/** Persiste os segmentos transcritos. Chamado ANTES de o áudio ser destruído (regra 2). */
class RoomSegmentSink(private val dao: SegmentDao) : SegmentSink {
    override suspend fun persist(result: TranscriptResult) {
        dao.insertAll(result.toSegmentEntities())
    }
}

/** Registra os gaps da timeline (overflow, áudio ausente, falha de transcrição). */
class RoomGapSink(private val dao: TimelineGapDao) : GapSink {
    override suspend fun recordGap(gap: TranscriptionGap) {
        dao.insert(
            TimelineGapEntity(
                id = "gap:${gap.reason.name}:${gap.fromMs}-${gap.toMs}",
                fromMs = gap.fromMs,
                toMs = gap.toMs,
                reason = gap.reason.name,
            ),
        )
    }
}
