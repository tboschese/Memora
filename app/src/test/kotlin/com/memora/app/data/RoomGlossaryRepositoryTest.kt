package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.db.MemoraDatabase
import com.memora.core.glossary.GlossaryEntry
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
 * Round-trip real do glossário no Room: save → observe (ordenado por canônica) → delete, com as
 * variantes serializadas. Complementa os testes puros de `GlossaryEditor`/`GlossaryCorrector`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomGlossaryRepositoryTest {

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

    @Test
    fun `save and observe round-trips an entry with variants`() = runBlocking {
        val repo = RoomGlossaryRepository(db.glossaryDao())
        repo.save(GlossaryEntry(id = "g1", canonical = "Kubernetes", variants = listOf("kubernetis", "cubernetes")))

        val entry = repo.observeAll().first().single()
        assertEquals("Kubernetes", entry.canonical)
        assertEquals(listOf("kubernetis", "cubernetes"), entry.variants)
    }

    @Test
    fun `entries are observed ordered by canonical`() = runBlocking {
        val repo = RoomGlossaryRepository(db.glossaryDao())
        repo.save(GlossaryEntry(id = "b", canonical = "Zulu"))
        repo.save(GlossaryEntry(id = "a", canonical = "Alpha"))

        assertEquals(listOf("Alpha", "Zulu"), repo.observeAll().first().map { it.canonical })
    }

    @Test
    fun `an entry without variants round-trips to an empty list`() = runBlocking {
        val repo = RoomGlossaryRepository(db.glossaryDao())
        repo.save(GlossaryEntry(id = "g1", canonical = "Memora"))

        assertTrue(repo.observeAll().first().single().variants.isEmpty())
    }

    @Test
    fun `save upserts by id and delete removes`() = runBlocking {
        val repo = RoomGlossaryRepository(db.glossaryDao())
        repo.save(GlossaryEntry(id = "g1", canonical = "Memora", variants = listOf("memra")))
        repo.save(GlossaryEntry(id = "g1", canonical = "Memora", variants = listOf("memra", "memoraa")))

        assertEquals(2, repo.observeAll().first().single().variants.size) // atualizado, não duplicado

        repo.delete("g1")
        assertTrue(repo.observeAll().first().isEmpty())
    }
}
