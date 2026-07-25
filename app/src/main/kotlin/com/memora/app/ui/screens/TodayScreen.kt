package com.memora.app.ui.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.memora.app.ui.AppViewModel
import com.memora.app.ui.HomeViewModel
import com.memora.core.common.timeline.DayItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Tela "Hoje": a timeline unificada do dia (por enquanto, as anotações) e um campo para escrever
 * uma nota nova. O `HomeViewModel` é criado com o banco da sessão, aberto no unlock.
 */
@Composable
fun TodayScreen(appViewModel: AppViewModel) {
    val home: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { appViewModel.createHomeViewModel() } },
    )
    val items by home.items.collectAsState()
    var noteText by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Hoje", style = MaterialTheme.typography.headlineMedium)

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
                items(items, key = { it.itemKey() }) { item -> DayItemRow(item) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nova anotação") },
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
}

@Composable
private fun DayItemRow(item: DayItem) {
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(time, style = MaterialTheme.typography.labelLarge)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun DayItem.itemKey(): String = when (this) {
    is DayItem.Speech -> "s:$id"
    is DayItem.UserNote -> "n:$id"
    is DayItem.Gap -> "g:$atMs:$toMs"
}
