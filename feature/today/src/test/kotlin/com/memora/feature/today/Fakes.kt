package com.memora.feature.today

import com.memora.core.common.time.DayRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fakes em memória para exercitar o ViewModel sem device, banco nem rede (princípio 6). Cada fonte
 * é um `MutableStateFlow` que o teste empurra à vontade.
 */
class FakeTodayRepository(
    private val utterances: MutableStateFlow<List<TodayItem.Utterance>> = MutableStateFlow(emptyList()),
    private val gaps: MutableStateFlow<List<TodayItem.Gap>> = MutableStateFlow(emptyList()),
) : TodayRepository {
    override fun observeUtterances(range: DayRange): Flow<List<TodayItem.Utterance>> = utterances
    override fun observeGaps(range: DayRange): Flow<List<TodayItem.Gap>> = gaps

    fun emitUtterances(value: List<TodayItem.Utterance>) { utterances.value = value }
    fun emitGaps(value: List<TodayItem.Gap>) { gaps.value = value }
}

/** Captura fake: `start`/`stop` só viram o flag observável. */
class FakeCaptureController : CaptureController {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    override val capturedCount: StateFlow<Int> = MutableStateFlow(0)
    override val droppedCount: StateFlow<Int> = MutableStateFlow(0)

    override fun start() { _isRecording.value = true }
    override fun stop() { _isRecording.value = false }
}
