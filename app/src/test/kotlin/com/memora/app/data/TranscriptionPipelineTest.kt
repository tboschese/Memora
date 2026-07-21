package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.common.model.AudioChunk
import com.memora.core.db.MemoraDatabase
import com.memora.core.transcription.AudioChunkAccess
import com.memora.core.transcription.DeviceState
import com.memora.core.transcription.DrainMode
import com.memora.core.transcription.GapReason
import com.memora.core.transcription.PendingChunk
import com.memora.core.transcription.TranscriptionQueue
import com.memora.core.transcription.fake.FakeTranscriptionProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Integração real do pipeline de transcrição contra um Room in-memory (sem SQLCipher): a fila
 * (:core:transcription) drena um chunk, o [RoomSegmentSink] persiste o segmento no banco (:core:db)
 * e só então o áudio é destruído. Prova, ponta a ponta, que os adaptadores casam com os DAOs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TranscriptionPipelineTest {

    private lateinit var db: MemoraDatabase

    /** AudioChunkAccess in-memory: guarda o PCM e registra o que foi destruído. */
    private class MemAudio : AudioChunkAccess {
        val stored = LinkedHashMap<String, AudioChunk>()
        val destroyed = mutableListOf<String>()

        fun put(chunkId: String, durationMs: Long) {
            stored[chunkId] = AudioChunk(chunkId, ShortArray(4), 16_000, Instant.EPOCH, durationMs)
        }

        override suspend fun load(chunkId: String) = stored[chunkId]
        override suspend fun destroy(chunkId: String) {
            stored.remove(chunkId)
            destroyed += chunkId
        }
    }

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
    fun `chunk drenado vira segmento no banco e o audio e destruido`() = runBlocking {
        val audio = MemAudio().apply { put("c1", durationMs = 1_000) }
        val queue = TranscriptionQueue(
            audio = audio,
            provider = FakeTranscriptionProvider(),
            segments = RoomSegmentSink(db.segmentDao()),
            gaps = RoomGapSink(db.timelineGapDao()),
        )

        queue.enqueue(PendingChunk("c1", startedAtMs = 0, durationMs = 1_000, sizeBytes = 10))
        val done = queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = true, idle = false))

        assertEquals(1, done)
        val rows = db.segmentDao().observeInRange(0, Long.MAX_VALUE).first()
        assertEquals(1, rows.size)
        assertEquals("c1:0", rows.single().id)
        assertEquals("UNKNOWN", rows.single().speaker)
        // Áudio destruído só depois do texto persistido.
        assertEquals(listOf("c1"), audio.destroyed)
        assertNull(audio.load("c1"))
    }

    @Test
    fun `overflow persiste um gap na timeline`() = runBlocking {
        val audio = MemAudio().apply { put("a", 500); put("b", 500); put("c", 500) }
        val queue = TranscriptionQueue(
            audio = audio,
            provider = FakeTranscriptionProvider(),
            segments = RoomSegmentSink(db.segmentDao()),
            gaps = RoomGapSink(db.timelineGapDao()),
            maxQueueBytes = 120,
        )

        queue.enqueue(PendingChunk("a", startedAtMs = 0, durationMs = 500, sizeBytes = 50))
        queue.enqueue(PendingChunk("b", startedAtMs = 500, durationMs = 500, sizeBytes = 50))
        queue.enqueue(PendingChunk("c", startedAtMs = 1_000, durationMs = 500, sizeBytes = 100))

        val gaps = db.timelineGapDao().observeInRange(0, Long.MAX_VALUE).first()
        assertEquals(1, gaps.size)
        assertEquals(GapReason.QUEUE_OVERFLOW.name, gaps.single().reason)
        assertEquals(0, gaps.single().fromMs)
        assertEquals(1_000, gaps.single().toMs)
    }
}
