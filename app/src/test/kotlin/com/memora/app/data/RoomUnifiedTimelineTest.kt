package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.common.time.DayRange
import com.memora.core.common.timeline.DayItem
import com.memora.core.db.MemoraDatabase
import com.memora.core.db.entity.NoteEntity
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.TimelineGapEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prova a timeline unificada de ponta a ponta sobre o Room: falas + notas + gaps do dia entram, uma
 * sequência cronológica única sai, respeitando o recorte do dia.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomUnifiedTimelineTest {

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
    fun `interleaves speech, notes and gaps chronologically within the day`() = runBlocking {
        db.segmentDao().insertAll(
            listOf(
                SegmentEntity(id = "s", chunkId = "c", text = "fala", startMs = 300, endMs = 310, confidence = 0.9f, language = "pt", speaker = "SELF"),
                SegmentEntity(id = "out", chunkId = "c", text = "fora", startMs = 50, endMs = 60, confidence = 0.9f, language = "pt"),
            ),
        )
        db.noteDao().upsert(NoteEntity(id = "n", text = "nota", createdAtMs = 100, tags = "tarefa ideia"))
        db.timelineGapDao().insert(TimelineGapEntity(id = "g", fromMs = 200, toMs = 210, reason = "AUDIO_MISSING"))

        val timeline = RoomUnifiedTimeline(db.segmentDao(), db.noteDao(), db.timelineGapDao())
        val items = timeline.observe(DayRange(fromMs = 100, toMs = 400)).first()

        assertEquals(listOf(100L, 200L, 300L), items.map { it.atMs }) // "out" (50) fica de fora
        assertEquals(DayItem.UserNote::class, items[0]::class)
        assertEquals(listOf("tarefa", "ideia"), (items[0] as DayItem.UserNote).tags)
        assertEquals(DayItem.Gap::class, items[1]::class)
        assertEquals(DayItem.Speech::class, items[2]::class)
        assertEquals("SELF", (items[2] as DayItem.Speech).speaker)
    }
}
