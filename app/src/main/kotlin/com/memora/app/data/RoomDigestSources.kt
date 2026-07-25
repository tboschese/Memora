package com.memora.app.data

import com.memora.core.common.model.SpeakerLabel
import com.memora.core.common.time.DayRange
import com.memora.core.db.dao.NoteDao
import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.digest.DigestSource
import com.memora.feature.digest.DigestSources
import kotlinx.coroutines.flow.first

/**
 * Fornece as linhas do dia para o digest a partir das falas (`SegmentDao`) **e** das anotações
 * (`NoteDao`) — o resumo do dia considera o que foi dito e o que o usuário anotou. Fica em `:app`, a
 * raiz de composição — assim `:feature:digest` não depende de `:core:db`. Tira um instantâneo do
 * intervalo (`first()`), já que o digest é gerado sob demanda, e intercala as fontes por tempo. As
 * notas entram como fala do dono (`SELF`): são a voz explícita do usuário no dia.
 */
class RoomDigestSources(
    private val segments: SegmentDao,
    private val notes: NoteDao,
) : DigestSources {
    override suspend fun forDay(range: DayRange): List<DigestSource> {
        val fromSegments = segments.observeInRange(range.fromMs, range.toMs).first().map(SegmentEntity::toDigestSource)
        val fromNotes = notes.observeInRange(range.fromMs, range.toMs).first().map(NoteEntity::toDigestSource)
        return (fromSegments + fromNotes).sortedBy { it.timeMs }
    }
}

internal fun SegmentEntity.toDigestSource(): DigestSource = DigestSource(
    timeMs = startMs,
    speaker = speaker.toSpeakerLabel(),
    text = text,
    place = place,
)

internal fun NoteEntity.toDigestSource(): DigestSource = DigestSource(
    timeMs = createdAtMs,
    speaker = SpeakerLabel.SELF, // a anotação é a voz explícita do dono
    text = text,
    place = place,
    tags = if (tags.isEmpty()) emptyList() else tags.split(" ").filter { it.isNotBlank() },
)
