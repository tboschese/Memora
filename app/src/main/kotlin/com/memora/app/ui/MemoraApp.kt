package com.memora.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.memora.app.session.SessionPhase
import com.memora.app.ui.screens.OnboardingScreen
import com.memora.app.ui.screens.TodayScreen
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
        val phase by appViewModel.phase.collectAsState()
        when (phase) {
            SessionPhase.ONBOARDING -> OnboardingScreen(appViewModel.onboarding)
            SessionPhase.LOCKED -> UnlockScreen(appViewModel.unlock)
            SessionPhase.UNLOCKED -> TodayScreen(appViewModel)
        }
    }
}
