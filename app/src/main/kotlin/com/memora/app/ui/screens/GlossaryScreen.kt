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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.memora.app.ui.GlossaryViewModel
import com.memora.core.glossary.GlossaryEntry

/**
 * Gerência do glossário: lista os termos (grafia canônica + variantes) e permite adicionar/remover.
 * Sub-tela acessada dos Ajustes; [onBack] volta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(viewModel: GlossaryViewModel, onBack: () -> Unit) {
    val entries by viewModel.entries.collectAsState()
    var canonical by remember { mutableStateOf("") }
    var variants by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glossário") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Voltar") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Grafias que a transcrição deve corrigir (ex.: “Kubernetes” com variantes “kubernetis, cubernetes”).",
                style = MaterialTheme.typography.bodySmall,
            )

            OutlinedTextField(
                value = canonical,
                onValueChange = { canonical = it },
                label = { Text("Grafia correta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = variants,
                onValueChange = { variants = it },
                label = { Text("Variantes (separadas por vírgula)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    viewModel.add(canonical, variants)
                    canonical = ""
                    variants = ""
                },
                enabled = canonical.isNotBlank(),
            ) {
                Text("Adicionar")
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = GlossaryEntry::id) { entry -> GlossaryRow(entry, viewModel::delete) }
            }
        }
    }
}

@Composable
private fun GlossaryRow(entry: GlossaryEntry, onDelete: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.canonical, style = MaterialTheme.typography.bodyLarge)
            if (entry.variants.isNotEmpty()) {
                Text(entry.variants.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = { onDelete(entry.id) }) { Text("✕") }
    }
}
