package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.common.time.DayRange
import com.memora.core.db.MemoraDatabase
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.feature.search.SearchQueryParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Busca ponta a ponta sobre o Room: falas + notas do dia projetadas e filtradas pela query. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomSearchIndexTest {

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

    private fun segment(id: String, text: String, at: Long, speaker: String = "SELF") =
        SegmentEntity(id = id, chunkId = "c", text = text, startMs = at, endMs = at + 5, confidence = 0.9f, language = "pt", speaker = speaker)

    @Test
    fun `finds matching speech and notes, newest first`() = runBlocking {
        db.segmentDao().insertAll(listOf(segment("s1", "reunião de orçamento", at = 100)))
        db.noteDao().upsert(NoteEntity(id = "n1", text = "revisar orçamento", createdAtMs = 200, tags = "tarefa"))
        db.noteDao().upsert(NoteEntity(id = "n2", text = "almoço", createdAtMs = 300))

        val index = RoomSearchIndex(db.segmentDao(), db.noteDao())
        val result = index.searchDay(SearchQueryParser.parse("orçamento"), DayRange(0, 1_000))

        assertEquals(listOf("n1", "s1"), result.map { it.id }) // 200 antes de 100 (desc)
    }

    @Test
    fun `tag filter matches note tags`() = runBlocking {
        db.noteDao().upsert(NoteEntity(id = "n1", text = "algo", createdAtMs = 10, tags = "tarefa"))
        db.noteDao().upsert(NoteEntity(id = "n2", text = "algo", createdAtMs = 20, tags = "ideia"))

        val index = RoomSearchIndex(db.segmentDao(), db.noteDao())
        val result = index.searchDay(SearchQueryParser.parse("algo #tarefa"), DayRange(0, 100))

        assertEquals(listOf("n1"), result.map { it.id })
    }

    @Test
    fun `speaker filter matches speech`() = runBlocking {
        db.segmentDao().insertAll(
            listOf(segment("self", "oi", at = 10, speaker = "SELF"), segment("other", "oi", at = 20, speaker = "OTHER")),
        )
        val index = RoomSearchIndex(db.segmentDao(), db.noteDao())
        val result = index.searchDay(SearchQueryParser.parse("oi @other"), DayRange(0, 100))

        assertEquals(listOf("other"), result.map { it.id })
    }

    @Test
    fun `an empty query returns nothing`() = runBlocking {
        db.segmentDao().insertAll(listOf(segment("s1", "qualquer", at = 10)))
        val index = RoomSearchIndex(db.segmentDao(), db.noteDao())
        assertTrue(index.searchDay(SearchQueryParser.parse("   "), DayRange(0, 100)).isEmpty())
    }

    @Test
    fun `searchAll spans every day, newest first`() = runBlocking {
        // dois "dias" bem distantes no tempo, ambos com "orçamento"
        db.segmentDao().insertAll(listOf(segment("ontem", "orçamento de ontem", at = 1_000)))
        db.noteDao().upsert(NoteEntity(id = "hoje", text = "orçamento de hoje", createdAtMs = 9_000_000))

        val index = RoomSearchIndex(db.segmentDao(), db.noteDao())
        val result = index.searchAll(SearchQueryParser.parse("orçamento"))

        assertEquals(listOf("hoje", "ontem"), result.map { it.id }) // full-history, mais recente antes
    }
}
