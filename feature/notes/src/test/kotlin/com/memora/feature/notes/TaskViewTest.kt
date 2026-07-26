package com.memora.feature.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskViewTest {

    private fun note(id: String, at: Long, tags: List<String> = listOf("tarefa"), done: Boolean = false) =
        Note(id = id, text = id, createdAtMs = at, tags = tags, done = done)

    @Test
    fun `keeps only task-tagged notes`() {
        val result = TaskView.openFirst(
            listOf(note("t", 1), note("plain", 2, tags = listOf("ideia"))),
        )
        assertEquals(listOf("t"), result.map { it.id })
    }

    @Test
    fun `pending come before done`() {
        val result = TaskView.openFirst(
            listOf(note("done", 5, done = true), note("open", 1)),
        )
        assertEquals(listOf("open", "done"), result.map { it.id })
    }

    @Test
    fun `within a group, most recent first`() {
        val result = TaskView.openFirst(
            listOf(note("old", 1), note("new", 3), note("mid", 2)),
        )
        assertEquals(listOf("new", "mid", "old"), result.map { it.id })
    }

    @Test
    fun `no tasks yields empty`() {
        assertEquals(emptyList<Note>(), TaskView.openFirst(listOf(note("x", 1, tags = emptyList()))))
    }
}
