package com.memora.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidades do banco local (Room sobre SQLCipher). Regra 3: **só texto persiste** — nenhum PCM
 * cru vive aqui. Timestamps são epoch-millis (Long) para ordenação/consulta por dia baratas.
 *
 * ⚠️ Compila apenas com o toolchain Android (Room + KSP); não foi compilado no ambiente de
 * desenvolvimento atual. As migrations reais entram junto com a chave do SQLCipher na Fase 1.
 */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAtMs: Long,
    /** null enquanto a sessão de captura está aberta. */
    val endedAtMs: Long? = null,
    /** Lugar nomeado vigente (offline), quando conhecido. */
    val place: String? = null,
)

/**
 * Um segmento de transcrição. Vem de `TranscriptResult` (:core:transcription); a atribuição de
 * speaker é preenchida separadamente (Fase 2) e começa como `UNKNOWN` — nunca chutar.
 */
@Entity(
    tableName = "segment",
    indices = [Index("sessionId"), Index("startMs"), Index("chunkId")],
)
data class SegmentEntity(
    @PrimaryKey val id: String,
    /** Sessão de captura à qual o segmento pertence (null até o vínculo existir). */
    val sessionId: String? = null,
    /** Chunk de origem — rastreabilidade até o áudio (já destruído). */
    val chunkId: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float,
    val language: String,
    /** Rótulo de speaker persistido como String (`SpeakerLabel`); default UNKNOWN. */
    val speaker: String = "UNKNOWN",
    val place: String? = null,
)

/**
 * Intervalo perdido na timeline (`TranscriptionGap`). Materializa a regra "nada some em
 * silêncio": overflow de fila, áudio ausente ou falha de transcrição deixam rastro consultável.
 */
@Entity(tableName = "timeline_gap", indices = [Index("fromMs")])
data class TimelineGapEntity(
    @PrimaryKey val id: String,
    val fromMs: Long,
    val toMs: Long,
    /** Motivo (`GapReason`) persistido como String. */
    val reason: String,
)
