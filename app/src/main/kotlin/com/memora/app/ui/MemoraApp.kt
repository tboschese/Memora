package com.memora.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.memora.app.session.SessionPhase
import com.memora.app.ui.screens.MainScreen
import com.memora.app.ui.screens.OnboardingScreen
import com.memora.app.ui.screens.UnlockScreen
import com.memora.app.ui.theme.MemoraTheme

/**
 * Host de UI: navega por [SessionPhase] (o cérebro é o `SessionCoordinator`, via [AppViewModel]).
 * Sem PIN → onboarding; com PIN e trancado → desbloqueio; destrancado → a tela "Hoje". A captura
 * roda em segundo plano independentemente da fase (regra 4) quando o serviço real existir.
 */
@Composable
fun MemoraApp(appViewModel: AppViewModel = hiltViewModel()) {
    MemoraTheme {
        // Auto-lock (regra 4): ao voltar do segundo plano, tranca se ficou fora além do timeout.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> appViewModel.onBackground()
                    Lifecycle.Event.ON_START -> appViewModel.onForeground()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val phase by appViewModel.phase.collectAsState()
        when (phase) {
            SessionPhase.ONBOARDING -> OnboardingScreen(appViewModel.onboarding)
            SessionPhase.LOCKED -> UnlockScreen(appViewModel.unlock)
            SessionPhase.UNLOCKED -> MainScreen(appViewModel)
        }
    }
}
