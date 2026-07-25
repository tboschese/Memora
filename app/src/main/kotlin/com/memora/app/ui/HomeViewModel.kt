package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.data.RoomUnifiedTimeline
import com.memora.core.common.time.DayRange
import com.memora.core.common.timeline.DayItem
import com.memora.feature.notes.Note
import com.memora.feature.notes.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Tela "Hoje" (pós-unlock): observa a timeline unificada do dia e grava anotações. Sem captura de
 * áudio ainda, a timeline nasce das próprias notas — o app já é usável (escrever e reler o dia).
 * Construído com o banco da sessão (aberto no unlock).
 */
class HomeViewModel(
    private val timeline: RoomUnifiedTimeline,
    private val notes: NotesRepository,
    private val newId: () -> String,
    private val now: () -> Long,
    clock: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val range: DayRange = DayRange.containing(clock, zone)

    val items: StateFlow<List<DayItem>> =
        timeline.observe(range).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    fun addNote(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            notes.add(Note(id = newId(), text = text.trim(), createdAtMs = now()))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
