package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.common.model.SpeakerLabel
import com.memora.core.db.MemoraDatabase
import com.memora.core.db.entity.SegmentEntity
import com.memora.core.db.entity.TimelineGapEntity
import com.memora.feature.today.DayRange
import com.memora.feature.today.TodayGapReason
import com.memora.feature.today.TodayItem
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
 * Prova que a leitura da tela "Hoje" lê o que o pipeline escreveu: entidades do Room viram
 * [TodayItem] do domínio, filtradas pelo intervalo do dia e mapeadas (speaker, motivo do gap).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomTodayRepositoryTest {

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
    fun `maps rows within the day range to domain items`() = runBlocking {
        val repo = RoomTodayRepository(db.segmentDao(), db.timelineGapDao())
        val range = DayRange(fromMs = 100, toMs = 200)

        db.segmentDao().insertAll(
            listOf(
                segment("in", startMs = 150, speaker = "SELF"),
                segment("before", startMs = 50),   // fora do intervalo
                segment("after", startMs = 250),   // fora do intervalo
            ),
        )
        db.timelineGapDao().insert(TimelineGapEntity(id = "g1", fromMs = 160, toMs = 170, reason = "AUDIO_MISSING"))

        val utterances = repo.observeUtterances(range).first()
        assertEquals(listOf("in"), utterances.map { it.id })
        assertEquals(SpeakerLabel.SELF, utterances.single().speaker)

        val gaps = repo.observeGaps(range).first()
        assertEquals(1, gaps.size)
        assertEquals(TodayGapReason.AUDIO_MISSING, gaps.single().reason)
    }

    @Test
    fun `unknown persisted labels degrade gracefully`() = runBlocking {
        val repo = RoomTodayRepository(db.segmentDao(), db.timelineGapDao())
        val range = DayRange(0, Long.MAX_VALUE)

        db.segmentDao().insertAll(listOf(segment("s", startMs = 1, speaker = "BOGUS")))
        db.timelineGapDao().insert(TimelineGapEntity(id = "g", fromMs = 2, toMs = 3, reason = "FUTURE_REASON"))

        assertEquals(SpeakerLabel.UNKNOWN, repo.observeUtterances(range).first().single().speaker)
        assertEquals(TodayGapReason.UNKNOWN, repo.observeGaps(range).first().single().reason)
    }
}
