package com.memora.feature.notes

import com.memora.core.common.time.DayRange
import kotlinx.coroutines.flow.Flow

/**
 * Leitura e escrita das anotações do dia. Interface no feature; a impl real (sobre o `NoteDao`) fica
 * em `:app` — o ViewModel roda com um fake e `:core:db` não vira dependência da UI.
 */
interface NotesRepository {
    fun observeInRange(range: DayRange): Flow<List<Note>>

    /** Todas as notas (para visões que cruzam dias, como a de tarefas). */
    fun observeAll(): Flow<List<Note>>

    suspend fun add(note: Note)

    suspend fun setDone(id: String, done: Boolean)

    suspend fun delete(id: String)
}
