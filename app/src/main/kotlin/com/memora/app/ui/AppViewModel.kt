package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.data.RoomDigestSources
import com.memora.app.data.RoomGlossaryRepository
import com.memora.app.data.RoomNotesRepository
import com.memora.app.data.RoomSearchIndex
import com.memora.app.data.RoomUnifiedTimeline
import com.memora.app.data.SessionDatabaseHolder
import com.memora.app.session.SessionCoordinator
import com.memora.app.session.SessionPhase
import com.memora.core.digest.fake.FakeDigestProvider
import com.memora.feature.digest.DigestViewModel
import com.memora.feature.onboarding.OnboardingViewModel
import com.memora.feature.onboarding.PinGate
import com.memora.feature.onboarding.SetupStep
import com.memora.feature.onboarding.UnlockViewModel
import com.memora.feature.settings.SettingsRepository
import com.memora.feature.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel raiz do app: expõe a [phase] da sessão para a navegação e hospeda os ViewModels de PIN
 * (reusando a lógica já testada de `:feature:onboarding`). Quando o setup/unlock conclui, avisa o
 * [SessionCoordinator] — que, com o banco já aberto pelo gate, faz a fase virar `UNLOCKED`.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    gate: PinGate,
    private val coordinator: SessionCoordinator,
    private val holder: SessionDatabaseHolder,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val onboarding = OnboardingViewModel(gate)
    val unlock = UnlockViewModel(gate)

    val phase: StateFlow<SessionPhase> = coordinator.phase

    init {
        viewModelScope.launch {
            onboarding.uiState.collect { if (it.step == SetupStep.DONE) coordinator.onAuthenticated() }
        }
        viewModelScope.launch {
            unlock.uiState.collect { if (it.isUnlocked) coordinator.onAuthenticated() }
        }
    }

    /** Cria o ViewModel da tela "Hoje" com o banco da sessão (só válido na fase UNLOCKED). */
    fun createHomeViewModel(): HomeViewModel {
        val db = holder.database
        return HomeViewModel(
            timeline = RoomUnifiedTimeline(db.segmentDao(), db.noteDao(), db.timelineGapDao()),
            notes = RoomNotesRepository(db.noteDao()),
            newId = { UUID.randomUUID().toString() },
            now = { System.currentTimeMillis() },
        )
    }

    /** ViewModel da tela de Digest. Usa o provider fake até o LLM local (whisper/llama) existir. */
    fun createDigestViewModel(): DigestViewModel {
        val db = holder.database
        return DigestViewModel(
            sources = RoomDigestSources(db.segmentDao(), db.noteDao()),
            provider = FakeDigestProvider(),
        )
    }

    /** ViewModel da tela de Busca sobre os dados do dia. */
    fun createSearchViewModel(): SearchViewModel =
        SearchViewModel(RoomSearchIndex(holder.database.segmentDao(), holder.database.noteDao()))

    /** ViewModel da tela de Ajustes. */
    fun createSettingsViewModel(): SettingsViewModel = SettingsViewModel(settingsRepository)

    /** ViewModel da gerência do glossário. */
    fun createGlossaryViewModel(): GlossaryViewModel = GlossaryViewModel(
        repository = RoomGlossaryRepository(holder.database.glossaryDao()),
        newId = { UUID.randomUUID().toString() },
    )

    /** Tranca a leitura manualmente (volta à tela de desbloqueio). A captura seguiria em background. */
    fun lock() = coordinator.lock()

    /** App foi para segundo plano: marca o instante para o auto-lock contar a partir daqui. */
    fun onBackground() = coordinator.onActivity()

    /** App voltou ao primeiro plano: aplica o timeout — tranca se ficou tempo demais fora. */
    fun onForeground() = coordinator.refresh()
}
