package com.memora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.memora.feature.onboarding.UnlockViewModel

/** Desbloqueio: confere o PIN e libera a leitura. Consome o `UnlockViewModel` já testado. */
@Composable
fun UnlockScreen(viewModel: UnlockViewModel) {
    val state by viewModel.uiState.collectAsState()
    var pin by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Desbloquear", style = MaterialTheme.typography.headlineMedium)
            Text("Digite seu PIN para ler o dia.", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.padding(top = 16.dp),
            )

            val locked = state.lockedForMs > 0
            when {
                locked -> Text(
                    "Muitas tentativas. Aguarde ~${(state.lockedForMs + 999) / 1000}s.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                state.error != null -> Text(
                    state.error!!.message(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Button(
                onClick = {
                    viewModel.submit(pin.toCharArray())
                    pin = ""
                },
                enabled = !state.isSubmitting && !locked,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text("Entrar")
            }
        }
    }
}
