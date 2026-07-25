package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.common.model.SpeakerLabel
import com.memora.core.common.time.DayRange
import com.memora.core.db.MemoraDatabase
import com.memora.core.db.entity.SegmentEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prova que a fonte do digest lê do banco o mesmo dia que a leitura de "Hoje": segmentos no
 * intervalo viram `DigestSource` ordenados por tempo, com o speaker mapeado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomDigestSourcesTest {

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

    private fun segment(id: String, startMs: Long, speaker: String = "UNKNOWN") = SegmentEntity(
        id = id, chunkId = "c", text = "t-$id", startMs = startMs, endMs = startMs + 10,
        confidence = 0.9f, language = "pt", speaker = speaker,
    )

    @Test
    fun `returns the day's segments as ordered digest sources`() = runBlocking {
        val sources = RoomDigestSources(db.segmentDao(), db.noteDao())
        db.segmentDao().insertAll(
            listOf(
                segment("late", startMs = 180, speaker = "SELF"),
                segment("early", startMs = 120),
                segment("before", startMs = 50),  // fora do intervalo
            ),
        )

        val result = sources.forDay(DayRange(fromMs = 100, toMs = 200))

        assertEquals(listOf(120L, 180L), result.map { it.timeMs })   // ordenado, sem o de fora
        assertEquals(SpeakerLabel.SELF, result.last().speaker)
    }

    @Test
    fun `interleaves notes as SELF sources with speech, ordered by time`() = runBlocking {
        val sources = RoomDigestSources(db.segmentDao(), db.noteDao())
        db.segmentDao().insertAll(listOf(segment("fala", startMs = 150, speaker = "OTHER")))
        db.noteDao().upsert(
            com.memora.core.db.entity.NoteEntity(id = "nota", text = "lembrete", createdAtMs = 130),
        )

        val result = sources.forDay(DayRange(fromMs = 100, toMs = 200))

        assertEquals(listOf(130L, 150L), result.map { it.timeMs })         // nota antes da fala
        assertEquals(SpeakerLabel.SELF, result.first().speaker)            // nota = SELF
        assertEquals("lembrete", result.first().text)
        assertEquals(SpeakerLabel.OTHER, result.last().speaker)
    }

    @Test
    fun `empty range yields no sources`() = runBlocking {
        val sources = RoomDigestSources(db.segmentDao(), db.noteDao())
        assertEquals(emptyList<Any>(), sources.forDay(DayRange(0, 100)))
    }
}
