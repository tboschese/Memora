package com.memora.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.memora.feature.settings.MemoraSettings
import com.memora.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Conteúdo da aba "Ajustes": edita os parâmetros que já têm efeito hoje (auto-lock, hora do digest).
 * Os thresholds de VAD/speaker existem no modelo mas só entram em jogo com a captura de áudio, então
 * ficam de fora da UI por ora. Salvar normaliza; "Restaurar padrões" volta ao `DEFAULT`.
 */
@Composable
fun SettingsContent(
    viewModel: SettingsViewModel,
    onOpenGlossary: () -> Unit,
    onExportHistory: suspend () -> String,
    onImportHistory: suspend (String) -> Int,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val markdown = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                if (markdown != null) {
                    val count = onImportHistory(markdown)
                    Toast.makeText(context, "$count anotações importadas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var autoLockMin by remember(settings) { mutableStateOf((settings.autoLockTimeoutMs / 60_000).toString()) }
    var digestHour by remember(settings) { mutableStateOf(settings.digestTargetHour.toString()) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = autoLockMin,
            onValueChange = { autoLockMin = it.filter(Char::isDigit).take(3) },
            label = { Text("Auto-lock (minutos)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = digestHour,
            onValueChange = { digestHour = it.filter(Char::isDigit).take(2) },
            label = { Text("Hora do digest (0–23)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                viewModel.update(
                    settings.copy(
                        autoLockTimeoutMs = (autoLockMin.toLongOrNull() ?: 2L).coerceAtLeast(1) * 60_000,
                        digestTargetHour = digestHour.toIntOrNull() ?: MemoraSettings.DEFAULT.digestTargetHour,
                    ),
                )
            }) {
                Text("Salvar")
            }
            OutlinedButton(onClick = { viewModel.reset() }) {
                Text("Restaurar padrões")
            }
        }

        OutlinedButton(onClick = onOpenGlossary, modifier = Modifier.fillMaxWidth()) {
            Text("Gerenciar glossário")
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    val markdown = onExportHistory()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_TEXT, markdown)
                        putExtra(Intent.EXTRA_TITLE, "Memora — histórico")
                    }
                    context.startActivity(Intent.createChooser(send, "Exportar histórico"))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exportar todo o histórico")
        }

        OutlinedButton(onClick = { importLauncher.launch("text/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Importar de Markdown")
        }

        Text(
            "Os parâmetros de transcrição e voz entram em cena quando a captura de áudio existir.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
