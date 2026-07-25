package com.memora.app.data

import com.memora.core.common.time.DayRange
import com.memora.core.db.dao.NoteDao
import com.memora.core.db.dao.SegmentDao
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.feature.search.SearchDocument
import com.memora.feature.search.SearchMatcher
import com.memora.feature.search.SearchQuery
import kotlinx.coroutines.flow.first

/**
 * Busca sobre os dados: projeta falas e notas em [SearchDocument] e aplica o [SearchMatcher] de
 * referência. Fica em `:app` — `:feature:search` não conhece `:core:db`. [searchAll] varre todo o
 * histórico; [searchDay] restringe a um dia. Instantâneos sob demanda; a aceleração por FTS do Room
 * entra depois sobre o mesmo contrato de matching.
 */
class RoomSearchIndex(
    private val segments: SegmentDao,
    private val notes: NoteDao,
) {
    suspend fun searchAll(query: SearchQuery): List<SearchDocument> {
        val docs = segments.snapshotAll().map(SegmentEntity::toSearchDocument) +
            notes.snapshotAll().map(NoteEntity::toSearchDocument)
        return SearchMatcher.match(docs, query)
    }

    suspend fun searchDay(query: SearchQuery, range: DayRange): List<SearchDocument> {
        val docs = segments.observeInRange(range.fromMs, range.toMs).first().map(SegmentEntity::toSearchDocument) +
            notes.observeInRange(range.fromMs, range.toMs).first().map(NoteEntity::toSearchDocument)
        return SearchMatcher.match(docs, query)
    }
}

internal fun SegmentEntity.toSearchDocument(): SearchDocument = SearchDocument(
    id = id,
    text = text,
    timeMs = startMs,
    speaker = speaker,
)

internal fun NoteEntity.toSearchDocument(): SearchDocument = SearchDocument(
    id = id,
    text = text,
    timeMs = createdAtMs,
    tags = if (tags.isEmpty()) emptyList() else tags.split(" ").filter { it.isNotBlank() },
)
