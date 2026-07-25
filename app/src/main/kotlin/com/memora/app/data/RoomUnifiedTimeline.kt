package com.memora.app.data

import com.memora.core.common.time.DayRange
import com.memora.core.common.timeline.DayItem
import com.memora.core.common.timeline.DayTimeline
import com.memora.core.db.dao.NoteDao
import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.dao.TimelineGapDao
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.TimelineGapEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Compõe a timeline unificada do dia a partir das três tabelas (falas, notas, gaps) e as intercala
 * com [DayTimeline]. Fica em `:app`, a raiz de composição — é aqui que as fontes de features
 * distintas se encontram, sem que `:feature:today` e `:feature:notes` dependam um do outro. Reativa:
 * qualquer escrita numa das tabelas re-emite a timeline.
 */
class RoomUnifiedTimeline(
    private val segments: SegmentDao,
    private val notes: NoteDao,
    private val gaps: TimelineGapDao,
) {
    fun observe(range: DayRange): Flow<List<DayItem>> =
        combine(
            segments.observeInRange(range.fromMs, range.toMs),
            notes.observeInRange(range.fromMs, range.toMs),
            gaps.observeInRange(range.fromMs, range.toMs),
        ) { segmentRows, noteRows, gapRows ->
            DayTimeline.merge(
                segmentRows.map(SegmentEntity::toDayItem),
                noteRows.map(NoteEntity::toDayItem),
                gapRows.map(TimelineGapEntity::toDayItem),
            )
        }
}

internal fun SegmentEntity.toDayItem(): DayItem.Speech = DayItem.Speech(
    id = id,
    atMs = startMs,
    text = text,
    speaker = speaker,
    place = place,
)

internal fun NoteEntity.toDayItem(): DayItem.UserNote = DayItem.UserNote(
    id = id,
    atMs = createdAtMs,
    text = text,
    tags = if (tags.isEmpty()) emptyList() else tags.split(" ").filter { it.isNotBlank() },
)

internal fun TimelineGapEntity.toDayItem(): DayItem.Gap = DayItem.Gap(
    atMs = fromMs,
    toMs = toMs,
    reason = reason,
)
