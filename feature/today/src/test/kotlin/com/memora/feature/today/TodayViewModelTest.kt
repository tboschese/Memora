package com.memora.feature.today

import app.cash.turbine.test
import com.memora.core.common.model.SpeakerLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun utterance(id: String, startMs: Long) =
        TodayItem.Utterance(id, "t-$id", startMs, startMs + 10, SpeakerLabel.UNKNOWN)

    private fun viewModel(repo: FakeTodayRepository, capture: FakeCaptureController) =
        TodayViewModel(repo, capture, now = Instant.EPOCH, zone = ZoneOffset.UTC)

    @Test
    fun `initial value is the loading state`() {
        // Sincronamente, antes de qualquer coleta: o valor semente é o carregamento.
        val vm = viewModel(FakeTodayRepository(), FakeCaptureController())
        assertTrue(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isEmpty)
    }

    @Test
    fun `merges utterances and gaps into a chronological state`() = runTest(dispatcher) {
        val repo = FakeTodayRepository()
        val vm = viewModel(repo, FakeCaptureController())

        vm.uiState.test {
            repo.emitUtterances(listOf(utterance("a", 300), utterance("b", 100)))
            repo.emitGaps(listOf(TodayItem.Gap(200, 210, TodayGapReason.AUDIO_MISSING)))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(listOf(100L, 200L, 300L), state.items.map { it.atMs })
        }
    }

    @Test
    fun `empty day is distinct from loading once sources emit`() = runTest(dispatcher) {
        val repo = FakeTodayRepository()
        val vm = viewModel(repo, FakeCaptureController())

        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `toggleRecording reflects capture state`() = runTest(dispatcher) {
        val capture = FakeCaptureController()
        val vm = viewModel(FakeTodayRepository(), capture)

        vm.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isRecording)

            vm.toggleRecording()
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().isRecording)

            vm.toggleRecording()
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isRecording)
        }
    }
}
