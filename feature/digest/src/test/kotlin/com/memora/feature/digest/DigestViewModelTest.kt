package com.memora.feature.digest

import com.memora.core.common.model.SpeakerLabel
import com.memora.core.digest.DigestSource
import com.memora.core.digest.fake.FakeDigestProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DigestViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun source(text: String, timeMs: Long) =
        DigestSource(timeMs = timeMs, speaker = SpeakerLabel.UNKNOWN, text = text)

    private fun viewModel(
        sources: DigestSources,
        provider: FakeDigestProvider = FakeDigestProvider(),
        glossary: List<String> = emptyList(),
    ) = DigestViewModel(
        sources = sources,
        provider = provider,
        glossaryTerms = glossary,
        // 1970-01-11 UTC → epochDay 10
        now = Instant.ofEpochMilli(10L * 24 * 60 * 60 * 1000),
        zone = ZoneOffset.UTC,
    )

    @Test
    fun `starts idle`() {
        val vm = viewModel(FakeDigestSources())
        assertEquals(DigestUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `generate produces a digest from the day's sources`() = runTest(dispatcher) {
        val provider = FakeDigestProvider()
        val vm = viewModel(
            FakeDigestSources(listOf(source("oi", 1), source("tchau", 2))),
            provider,
            glossary = listOf("Memora"),
        )

        vm.generate()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DigestUiState.Ready)
        val digest = (state as DigestUiState.Ready).digest
        assertEquals(10L, digest.epochDay)
        assertEquals("2 trechos ao longo do dia.", digest.summary)
        // O epochDay e o glossário chegaram ao provider.
        assertEquals(10L, provider.lastInput?.epochDay)
        assertEquals(listOf("Memora"), provider.lastInput?.glossaryTerms)
    }

    @Test
    fun `an empty day is Empty and never calls the provider`() = runTest(dispatcher) {
        val provider = FakeDigestProvider()
        val vm = viewModel(FakeDigestSources(emptyList()), provider)

        vm.generate()
        advanceUntilIdle()

        assertEquals(DigestUiState.Empty, vm.uiState.value)
        assertEquals(null, provider.lastInput) // provider não foi chamado
    }

    @Test
    fun `a source failure degrades to Failed instead of crashing`() = runTest(dispatcher) {
        val vm = viewModel(FailingDigestSources())

        vm.generate()
        advanceUntilIdle()

        assertEquals(DigestUiState.Failed, vm.uiState.value)
    }
}
