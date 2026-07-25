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
import com.memora.feature.onboarding.OnboardingViewModel
import com.memora.feature.onboarding.SetupStep

/**
 * Onboarding: define o PIN em dois passos (escolher → confirmar). Consome o `OnboardingViewModel`
 * já testado; a UI só coleta o estado e chama `propose`/`confirm`.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsState()
    var pin by remember { mutableStateOf("") }

    val title = if (state.step == SetupStep.CONFIRM) "Confirme seu PIN" else "Crie um PIN"
    val action = if (state.step == SetupStep.CONFIRM) "Confirmar" else "Continuar"

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Memora", style = MaterialTheme.typography.headlineMedium)
            Text(
                "O PIN protege a leitura do seu dia. A captura roda em segundo plano.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.padding(top = 16.dp),
            )

            state.error?.let {
                Text(it.message(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    if (state.step == SetupStep.CONFIRM) {
                        viewModel.confirm(pin.toCharArray())
                    } else {
                        viewModel.propose(pin.toCharArray())
                    }
                    pin = ""
                },
                enabled = !state.isSubmitting,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(action)
            }
        }
    }
}
