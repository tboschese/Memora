package com.memora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.memora.app.ui.SearchViewModel
import com.memora.feature.search.SearchDocument
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Conteúdo da aba "Buscar": filtra falas e anotações do dia. Sintaxe leve — `#tag`, `@speaker` e
 * termos livres. Query vazia mostra a dica, não tudo.
 */
@Composable
fun SearchContent(viewModel: SearchViewModel, modifier: Modifier = Modifier) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Buscar", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Buscar no dia (#tag, @quem, termos)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )

        when {
            query.isBlank() -> Text("Digite para buscar nas falas e anotações de hoje.")
            results.isEmpty() -> Text("Nenhum resultado.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = SearchDocument::id) { doc -> SearchResultRow(doc) }
            }
        }
    }
}

private val WHEN: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

@Composable
private fun SearchResultRow(doc: SearchDocument) {
    Column {
        Text(
            WHEN.format(Instant.ofEpochMilli(doc.timeMs).atZone(ZoneId.systemDefault())),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(doc.text, style = MaterialTheme.typography.bodyMedium)
    }
}
