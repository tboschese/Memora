package com.memora.feature.notes

import app.cash.turbine.test
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
class NotesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    // 1970-01-11 UTC → dia [864_000_000, 950_400_000)
    private val dayMs = 10L * 24 * 60 * 60 * 1000

    private fun viewModel(repo: FakeNotesRepository, ids: Iterator<String>, nowMs: Long = dayMs) =
        NotesViewModel(
            repository = repo,
            newId = { ids.next() },
            now = { nowMs },
            clock = Instant.ofEpochMilli(dayMs),
            zone = ZoneOffset.UTC,
        )

    @Test
    fun `save persists a note and it shows up in the day's list`() = runTest(dispatcher) {
        val repo = FakeNotesRepository()
        val vm = viewModel(repo, listOf("n1").iterator())

        vm.uiState.test {
            assertTrue(vm.save(NoteDraft(text = "comprar pão", tags = listOf("tarefa"))))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.notes.size)
            assertEquals("comprar pão", state.notes.single().text)
            assertEquals(listOf("tarefa"), state.notes.single().tags)
            assertEquals("n1", state.notes.single().id)
        }
    }

    @Test
    fun `a blank draft is not saved`() = runTest(dispatcher) {
        val repo = FakeNotesRepository()
        val vm = viewModel(repo, emptyList<String>().iterator())

        assertFalse(vm.save(NoteDraft(text = "   ", tags = emptyList())))
        advanceUntilIdle()

        vm.uiState.test {
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().notes.isEmpty())
        }
    }

    @Test
    fun `delete removes a note`() = runTest(dispatcher) {
        val repo = FakeNotesRepository()
        val vm = viewModel(repo, listOf("n1").iterator())

        vm.uiState.test {
            vm.save(NoteDraft(text = "rascunho"))
            advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().notes.size)

            vm.delete("n1")
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().notes.isEmpty())
        }
    }

    @Test
    fun `notes outside the day are not shown`() = runTest(dispatcher) {
        val repo = FakeNotesRepository()
        // uma nota de ontem, injetada direto no store
        repo.add(Note(id = "old", text = "ontem", createdAtMs = dayMs - 1))
        val vm = viewModel(repo, listOf("n1").iterator())

        vm.uiState.test {
            vm.save(NoteDraft(text = "hoje"))
            advanceUntilIdle()

            val notes = expectMostRecentItem().notes
            assertEquals(listOf("hoje"), notes.map { it.text })
        }
    }
}
