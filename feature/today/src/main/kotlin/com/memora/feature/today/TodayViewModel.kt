package com.memora.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.core.common.time.DayRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

/**
 * ViewModel da tela "Hoje" (plano §4.4): a transcrição do dia em ordem cronológica + o card de
 * gravação. Só orquestra — combina as fontes ([TodayRepository]) e o estado de captura
 * ([CaptureController]) no [TodayUiState]; a junção cronológica é pura ([TodayTimeline]).
 *
 * O [range] do dia é fixado na construção (fuso do usuário). Suficiente para o MVP; a virada de dia
 * com o app aberto é um refinamento posterior.
 */
class TodayViewModel(
    private val repository: TodayRepository,
    private val capture: CaptureController,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val range: DayRange = DayRange.containing(now, zone)

    val uiState: StateFlow<TodayUiState> =
        combine(
            repository.observeUtterances(range),
            repository.observeGaps(range),
            capture.isRecording,
        ) { utterances, gaps, recording ->
            TodayUiState(
                items = TodayTimeline.merge(utterances, gaps),
                isRecording = recording,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = TodayUiState(),
        )

    /** Alterna a captura pelo card da tela (RF-01/RF-02). */
    fun toggleRecording() {
        if (capture.isRecording.value) capture.stop() else capture.start()
    }

    fun startRecording() = capture.start()

    fun stopRecording() = capture.stop()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
