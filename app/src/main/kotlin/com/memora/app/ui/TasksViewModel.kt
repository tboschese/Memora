package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.feature.notes.Note
import com.memora.feature.notes.NotesRepository
import com.memora.feature.notes.TaskView
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Visão de tarefas: todas as anotações `#tarefa`, de qualquer dia, ordenadas por [TaskView] (as
 * pendentes primeiro, mais recentes no topo). Marcar como concluída reordena. A seleção/ordenação é
 * pura ([TaskView]); aqui só se liga a fonte reativa e a escrita.
 */
class TasksViewModel(private val notes: NotesRepository) : ViewModel() {

    val tasks: StateFlow<List<Note>> = notes.observeAll()
        .map(TaskView::openFirst)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDone(id: String, done: Boolean) {
        viewModelScope.launch { notes.setDone(id, done) }
    }

    fun delete(id: String) {
        viewModelScope.launch { notes.delete(id) }
    }
}
