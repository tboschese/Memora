package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.data.RoomUnifiedTimeline
import com.memora.core.common.time.DayRange
import com.memora.core.common.timeline.DayItem
import com.memora.feature.notes.Note
import com.memora.feature.notes.NoteInput
import com.memora.feature.notes.NotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tela "Hoje" (pós-unlock): observa a timeline unificada do **dia selecionado** e grava anotações.
 * O diário é navegável no tempo (dia anterior/seguinte); anotar só vale para hoje (a nota carimba o
 * instante real). Sem captura de áudio ainda, a timeline nasce das próprias notas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val timeline: RoomUnifiedTimeline,
    private val notes: NotesRepository,
    private val newId: () -> String,
    private val now: () -> Long,
    clock: Instant = Instant.now(),
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val today: LocalDate = clock.atZone(zone).toLocalDate()

    private val _date = MutableStateFlow(today)
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    /** `true` quando o dia visível é hoje — só aí faz sentido anotar. */
    val isToday: StateFlow<Boolean> = _date
        .map { it == today }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val items: StateFlow<List<DayItem>> = _date
        .flatMapLatest { date -> timeline.observe(DayRange.of(date, zone)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun previousDay() {
        _date.value = _date.value.minusDays(1)
    }

    fun nextDay() {
        if (_date.value < today) _date.value = _date.value.plusDays(1)
    }

    fun goToToday() {
        _date.value = today
    }

    /** Cria uma nota do texto cru, extraindo `#tags` (RF-08). Ignora entrada vazia. */
    fun addNote(raw: String) {
        val draft = NoteInput.parse(raw)
        if (draft.isBlank) return
        viewModelScope.launch {
            notes.add(Note(id = newId(), text = draft.text, createdAtMs = now(), tags = draft.tags))
        }
    }

    /** Edita uma nota existente preservando seu instante ([atMs]); reparseia `#tags`. Texto vazio apaga. */
    fun editNote(id: String, atMs: Long, raw: String) {
        val draft = NoteInput.parse(raw)
        if (draft.isBlank) {
            deleteNote(id)
            return
        }
        viewModelScope.launch {
            notes.add(Note(id = id, text = draft.text, createdAtMs = atMs, tags = draft.tags))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { notes.delete(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
