package com.memora.app.data

import com.memora.core.common.time.DayRange
import com.memora.core.db.dao.NoteDao
import com.memora.core.db.entity.NoteEntity
import com.memora.feature.notes.Note
import com.memora.feature.notes.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistência das anotações sobre o `NoteDao`. Fica em `:app`, a raiz de composição — `:feature:notes`
 * não depende de `:core:db`. Traduz as tags entre a `List<String>` do domínio e a String
 * space-separated da entidade (tags são tokens sem espaço).
 */
class RoomNotesRepository(private val dao: NoteDao) : NotesRepository {

    override fun observeInRange(range: DayRange): Flow<List<Note>> =
        dao.observeInRange(range.fromMs, range.toMs).map { rows -> rows.map(NoteEntity::toNote) }

    override suspend fun add(note: Note) = dao.upsert(note.toEntity())

    override suspend fun setDone(id: String, done: Boolean) = dao.setDone(id, done)

    override suspend fun delete(id: String) = dao.deleteById(id)
}

internal fun NoteEntity.toNote(): Note = Note(
    id = id,
    text = text,
    createdAtMs = createdAtMs,
    tags = tags.toTagList(),
    segmentId = segmentId,
    place = place,
    done = done,
)

internal fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    text = text,
    createdAtMs = createdAtMs,
    segmentId = segmentId,
    tags = tags.joinToString(" "),
    place = place,
    done = done,
)

private fun String.toTagList(): List<String> =
    if (isBlank()) emptyList() else trim().split(" ").filter { it.isNotBlank() }
