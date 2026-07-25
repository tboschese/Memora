package com.memora.app.data

import com.memora.core.common.time.DayRange
import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.digest.DigestSource
import com.memora.feature.digest.DigestSources
import kotlinx.coroutines.flow.first

/**
 * Fornece as linhas do dia para o digest a partir do `SegmentDao`. Fica em `:app`, a raiz de
 * composição — assim `:feature:digest` não depende de `:core:db`. Tira um instantâneo do intervalo
 * (`first()`), já que o digest é gerado sob demanda, e mapeia entidade → `DigestSource` (o mesmo
 * mapa de speaker de `RoomTodayRepository`).
 */
class RoomDigestSources(private val segments: SegmentDao) : DigestSources {
    override suspend fun forDay(range: DayRange): List<DigestSource> =
        segments.observeInRange(range.fromMs, range.toMs).first().map(SegmentEntity::toDigestSource)
}

internal fun SegmentEntity.toDigestSource(): DigestSource = DigestSource(
    timeMs = startMs,
    speaker = speaker.toSpeakerLabel(),
    text = text,
    place = place,
)
