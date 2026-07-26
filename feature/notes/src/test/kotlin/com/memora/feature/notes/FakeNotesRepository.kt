package com.memora.feature.notes

import com.memora.core.common.time.DayRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repositório fake em memória. Filtra por [DayRange] como o real, para o ViewModel exercitar o
 * recorte do dia. Ignora timezone — o range já vem em epoch-millis.
 */
class FakeNotesRepository(
    private val store: MutableStateFlow<List<Note>> = MutableStateFlow(emptyList()),
) : NotesRepository {

    override fun observeInRange(range: DayRange): Flow<List<Note>> =
        store.map { notes ->
            notes.filter { it.createdAtMs >= range.fromMs && it.createdAtMs < range.toMs }
                .sortedBy { it.createdAtMs }
        }

    override suspend fun add(note: Note) {
        store.value = store.value.filterNot { it.id == note.id } + note
    }

    override suspend fun setDone(id: String, done: Boolean) {
        store.value = store.value.map { if (it.id == id) it.copy(done = done) else it }
    }

    override suspend fun delete(id: String) {
        store.value = store.value.filterNot { it.id == id }
    }
}
