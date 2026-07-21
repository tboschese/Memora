package com.memora.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.memora.app.ui.theme.MemoraTheme

/**
 * Host de UI (placeholder do M0). O NavHost real e as telas (Hoje/Digest/Buscar/…)
 * entram nas Fases 1–3 conforme os módulos :feature:* forem implementados.
 */
@Composable
fun MemoraApp() {
    MemoraTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "Memora", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Baseline M0 — captura, transcrição e digest chegam nas próximas fases.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoraAppPreview() {
    MemoraApp()
}
