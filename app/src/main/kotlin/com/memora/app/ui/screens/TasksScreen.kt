package com.memora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.memora.app.ui.TasksViewModel
import com.memora.feature.notes.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val WHEN: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

/**
 * Conteúdo da aba "Tarefas": todas as anotações `#tarefa` de qualquer dia, pendentes primeiro.
 * Marcar a caixa conclui e reordena.
 */
@Composable
fun TasksContent(viewModel: TasksViewModel, modifier: Modifier = Modifier) {
    val tasks by viewModel.tasks.collectAsState()
    var hideDone by remember { mutableStateOf(false) }
    val shown = if (hideDone) tasks.filterNot { it.done } else tasks

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val pending = tasks.count { !it.done }
        Text("Tarefas", style = MaterialTheme.typography.headlineMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (tasks.isEmpty()) "Sem tarefas. Anote com #tarefa na tela Hoje." else "$pending pendente(s).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (tasks.any { it.done }) {
                FilterChip(selected = hideDone, onClick = { hideDone = !hideDone }, label = { Text("Ocultar concluídas") })
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(shown, key = Note::id) { task -> TaskRow(task, viewModel::setDone) }
        }
    }
}

@Composable
private fun TaskRow(task: Note, onToggle: (String, Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id, it) })
        Text(
            task.text,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        Text(
            WHEN.format(Instant.ofEpochMilli(task.createdAtMs).atZone(ZoneId.systemDefault())),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
