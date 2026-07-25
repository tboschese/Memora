package com.memora.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.core.common.time.DayRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Estado da lista de anotações do dia. */
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel das anotações do dia (§5.1). Observa as notas do dia e cria novas a partir de um
 * [NoteDraft] — o id e o `createdAtMs` são atribuídos aqui (via seams testáveis [newId]/[now]),
 * então o rascunho da UI não carrega infra. Rascunho vazio é ignorado — nada de nota fantasma.
 */
class NotesViewModel(
    private val repository: NotesRepository,
    private val newId: () -> String,
    private val now: () -> Long = { System.currentTimeMillis() },
    clock: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val range: DayRange = DayRange.containing(clock, zone)

    val uiState: StateFlow<NotesUiState> =
        repository.observeInRange(range)
            .map { NotesUiState(notes = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = NotesUiState(),
            )

    /** Persiste o rascunho como nota nova. Ignora rascunhos vazios. Retorna se salvou. */
    fun save(draft: NoteDraft): Boolean {
        if (draft.isBlank) return false
        val note = Note(
            id = newId(),
            text = draft.text.trim(),
            createdAtMs = now(),
            tags = draft.tags,
            segmentId = draft.segmentId,
            place = draft.place,
        )
        viewModelScope.launch { repository.add(note) }
        return true
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
