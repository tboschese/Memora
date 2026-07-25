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

/**
 * Anotação do usuário (Fase 2, §5.1). Diferente de um `SegmentEntity`, nasce do usuário, não da
 * transcrição — mas divide a timeline por [createdAtMs] e pode ancorar num segmento ([segmentId],
 * o "timestamp exato" do RF-07). As tags rápidas (#reunião/#ideia/…) ficam separadas por espaço em
 * [tags] — são tokens sem espaço, então dispensam um TypeConverter; a camada de cima faz split/join.
 */
@Entity(tableName = "note", indices = [Index("createdAtMs"), Index("segmentId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAtMs: Long,
    /** Segmento ancorado, quando a nota foi feita durante a captura (null para nota avulsa). */
    val segmentId: String? = null,
    /** Tags rápidas serializadas (space-separated), "" quando não há. */
    val tags: String = "",
    val place: String? = null,
)

/**
 * Termo do glossário do usuário (Fase 2, §5.4): grafia canônica + variantes erradas a corrigir.
 * As [variants] ficam separadas por `\n` (podem ser multi-palavra, mas nunca contêm quebra de
 * linha) — dispensa TypeConverter; a camada de cima faz split/join. Ver `GlossaryEntry`.
 */
@Entity(tableName = "glossary", indices = [Index("canonical")])
data class GlossaryEntity(
    @PrimaryKey val id: String,
    val canonical: String,
    /** Variantes serializadas (newline-separated), "" quando não há. */
    val variants: String = "",
    val description: String? = null,
)
