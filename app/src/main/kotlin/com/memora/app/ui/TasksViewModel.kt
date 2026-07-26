package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.data.toDayItem
import com.memora.core.common.timeline.DayItem
import com.memora.core.db.dao.NoteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Visão de tarefas: todas as anotações marcadas `#tarefa`, de qualquer dia, com as **pendentes
 * primeiro** e, dentro de cada grupo, as mais recentes no topo. Marcar como concluída reordena.
 * Transforma o diário num gerenciador de tarefas simples.
 */
const val TASK_TAG = "tarefa"

class TasksViewModel(private val noteDao: NoteDao) : ViewModel() {

    val tasks: StateFlow<List<DayItem.UserNote>> = noteDao.observeAll()
        .map { rows ->
            rows.map { it.toDayItem() }
                .filter { TASK_TAG in it.tags }
                .sortedWith(compareBy({ it.done }, { -it.atMs }))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDone(id: String, done: Boolean) {
        viewModelScope.launch { noteDao.setDone(id, done) }
    }
}
