package com.memora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.memora.core.digest.Digest
import com.memora.feature.digest.DigestUiState
import com.memora.feature.digest.DigestViewModel

/**
 * Conteúdo da aba "Digest": gera, sob demanda, o resumo estruturado do dia. Usa o provider fake até
 * o LLM local existir — o fluxo (gerar → resumo/decisões/temas) já é o real.
 */
@Composable
fun DigestContent(viewModel: DigestViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Digest do dia", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = { viewModel.generate() }, enabled = state !is DigestUiState.Generating) {
            Text("Gerar digest")
        }

        when (val s = state) {
            is DigestUiState.Idle -> Text("Toque em “Gerar digest” para resumir o dia.")
            is DigestUiState.Generating -> CircularProgressIndicator()
            is DigestUiState.Empty -> Text("Nada para resumir ainda — sem falas ou anotações hoje.")
            is DigestUiState.Failed -> Text(
                "Não foi possível gerar o digest.",
                color = MaterialTheme.colorScheme.error,
            )
            is DigestUiState.Ready -> DigestBody(s.digest)
        }
    }
}

@Composable
private fun DigestBody(digest: Digest) {
    Text(digest.summary, style = MaterialTheme.typography.bodyLarge)
    Section("Decisões", digest.decisions)
    Section("Meus itens de ação", digest.myActionItems)
    Section("Temas", digest.themes)
}

@Composable
private fun Section(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
}
