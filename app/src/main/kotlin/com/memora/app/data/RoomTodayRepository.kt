package com.memora.app.data

import com.memora.core.common.model.SpeakerLabel
import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.dao.TimelineGapDao
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.TimelineGapEntity
import com.memora.feature.today.DayRange
import com.memora.feature.today.TodayGapReason
import com.memora.feature.today.TodayItem
import com.memora.feature.today.TodayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação de leitura da tela "Hoje" sobre os DAOs do Room. Fica em `:app`, a raiz de
 * composição — assim `:feature:today` não depende de `:core:db` e o ViewModel segue testável com um
 * fake. Espelha `RoomSegmentSink` (escrita): a fronteira banco↔UI se resolve aqui, não nos módulos.
 */
class RoomTodayRepository(
    private val segments: SegmentDao,
    private val gaps: TimelineGapDao,
) : TodayRepository {

    override fun observeUtterances(range: DayRange): Flow<List<TodayItem.Utterance>> =
        segments.observeInRange(range.fromMs, range.toMs).map { rows -> rows.map(SegmentEntity::toUtterance) }

    override fun observeGaps(range: DayRange): Flow<List<TodayItem.Gap>> =
        gaps.observeInRange(range.fromMs, range.toMs).map { rows -> rows.map(TimelineGapEntity::toGap) }
}

/** O speaker persistido é uma String livre; um rótulo fora do enum degrada para `UNKNOWN`. */
internal fun SegmentEntity.toUtterance(): TodayItem.Utterance = TodayItem.Utterance(
    id = id,
    text = text,
    startMs = startMs,
    endMs = endMs,
    speaker = speaker.toSpeakerLabel(),
    place = place,
)

internal fun TimelineGapEntity.toGap(): TodayItem.Gap = TodayItem.Gap(
    fromMs = fromMs,
    toMs = toMs,
    reason = TodayGapReason.fromPersisted(reason),
)

private fun String.toSpeakerLabel(): SpeakerLabel =
    SpeakerLabel.entries.firstOrNull { it.name == this } ?: SpeakerLabel.UNKNOWN
