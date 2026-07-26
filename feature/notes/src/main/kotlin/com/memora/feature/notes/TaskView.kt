package com.memora.feature.notes

/**
 * Visão de tarefas (pura): entre todas as anotações, seleciona as marcadas [TAG] e as ordena com as
 * **pendentes primeiro** e, dentro de cada grupo, as mais recentes no topo. Determinística e
 * testável — a fonte reativa (Room) e a UI ficam de fora.
 */
object TaskView {
    const val TAG = "tarefa"

    fun openFirst(notes: List<Note>): List<Note> =
        notes.filter { TAG in it.tags }
            .sortedWith(compareBy({ it.done }, { -it.createdAtMs }))
}
