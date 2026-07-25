package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.common.time.DayRange
import com.memora.core.db.MemoraDatabase
import com.memora.feature.notes.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Round-trip real das anotações no Room: add → observe → delete, com as tags serializadas e o
 * recorte por dia. Complementa os testes de ViewModel (fakes) com o schema/DAO de verdade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomNotesRepositoryTest {

    private lateinit var db: MemoraDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            MemoraDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun note(id: String, createdAtMs: Long, tags: List<String> = emptyList()) =
        Note(id = id, text = "t-$id", createdAtMs = createdAtMs, tags = tags)

    @Test
    fun `add and observe round-trips a note with tags`() = runBlocking {
        val repo = RoomNotesRepository(db.noteDao())
        repo.add(note("n1", createdAtMs = 150, tags = listOf("reuniao", "ideia")))

        val notes = repo.observeInRange(DayRange(100, 200)).first()
        assertEquals(1, notes.size)
        assertEquals(listOf("reuniao", "ideia"), notes.single().tags)
    }

    @Test
    fun `observe respects the day range`() = runBlocking {
        val repo = RoomNotesRepository(db.noteDao())
        repo.add(note("in", createdAtMs = 150))
        repo.add(note("before", createdAtMs = 50))
        repo.add(note("after", createdAtMs = 250))

        val notes = repo.observeInRange(DayRange(100, 200)).first()
        assertEquals(listOf("in"), notes.map { it.id })
    }

    @Test
    fun `delete removes the note`() = runBlocking {
        val repo = RoomNotesRepository(db.noteDao())
        repo.add(note("n1", createdAtMs = 150))
        repo.delete("n1")

        assertTrue(repo.observeInRange(DayRange(0, Long.MAX_VALUE)).first().isEmpty())
    }

    @Test
    fun `a note without tags round-trips to an empty list`() = runBlocking {
        val repo = RoomNotesRepository(db.noteDao())
        repo.add(note("n1", createdAtMs = 10))

        assertTrue(repo.observeInRange(DayRange(0, 100)).first().single().tags.isEmpty())
    }
}
