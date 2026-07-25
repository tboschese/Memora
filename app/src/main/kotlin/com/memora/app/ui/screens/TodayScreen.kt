package com.memora.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.memora.app.data.exportDayMarkdown
import com.memora.app.ui.HomeViewModel
import com.memora.core.common.timeline.DayItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Conteúdo da aba "Hoje": a timeline unificada do dia (por enquanto, as anotações) e um campo para
 * escrever uma nota nova. Sem `Scaffold` próprio — vive dentro do `MainScreen`.
 */
@Composable
fun TodayContent(home: HomeViewModel, modifier: Modifier = Modifier) {
    val items by home.items.collectAsState()
    var noteText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hoje", style = MaterialTheme.typography.headlineMedium)
            TextButton(
                onClick = {
                    val markdown = exportDayMarkdown(items, LocalDate.now(), emptyList(), ZoneId.systemDefault())
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_TEXT, markdown)
                        putExtra(Intent.EXTRA_TITLE, "Memora — ${LocalDate.now()}")
                    }
                    context.startActivity(Intent.createChooser(send, "Exportar o dia"))
                },
                enabled = items.isNotEmpty(),
            ) {
                Text("Exportar")
            }
        }

        if (items.isEmpty()) {
            Text(
                "Nada por aqui ainda. Escreva uma anotação abaixo.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.itemKey() }) { item ->
                DayItemRow(item, onDelete = home::deleteNote)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Nova anotação (use #tag)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    home.addNote(noteText)
                    noteText = ""
                },
                enabled = noteText.isNotBlank(),
            ) {
                Text("Anotar")
            }
        }
    }
}

@Composable
private fun DayItemRow(item: DayItem, onDelete: (String) -> Unit) {
    val time = TIME.format(Instant.ofEpochMilli(item.atMs).atZone(ZoneId.systemDefault()))
    val text = when (item) {
        is DayItem.Speech -> buildString {
            item.speaker?.takeIf { it != "UNKNOWN" }?.let { append("($it) ") }
            append(item.text)
        }
        is DayItem.UserNote -> "📝 " + item.text +
            if (item.tags.isNotEmpty()) "  " + item.tags.joinToString(" ") { "#$it" } else ""
        is DayItem.Gap -> "⋯ trecho sem áudio"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(time, style = MaterialTheme.typography.labelLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
        if (item is DayItem.UserNote) {
            TextButton(onClick = { onDelete(item.id) }) { Text("✕") }
        }
    }
}

private fun DayItem.itemKey(): String = when (this) {
    is DayItem.Speech -> "s:$id"
    is DayItem.UserNote -> "n:$id"
    is DayItem.Gap -> "g:$atMs:$toMs"
}
