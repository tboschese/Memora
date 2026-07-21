package com.memora.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.memora.core.audio.CapturePipeline
import com.memora.core.audio.EnergyVad
import com.memora.core.audio.FileEphemeralAudioStore
import com.memora.core.audio.PcmChunker
import com.memora.core.db.MemoraDatabase
import com.memora.core.transcription.DeviceState
import com.memora.core.transcription.DrainMode
import com.memora.core.transcription.PendingChunk
import com.memora.core.transcription.TranscriptionQueue
import com.memora.core.transcription.fake.FakeTranscriptionProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Capstone da Fase 1: prova que TODAS as peças compõem, de PCM cru ao banco.
 *
 * `CapturePipeline` (chunking + VAD + store efêmero cifrado) → `EphemeralAudioChunkAccess` →
 * `TranscriptionQueue` → `RoomSegmentSink` → `MemoraDatabase`. Verifica que a fala vira segmento
 * persistido, o silêncio é descartado, e o áudio é destruído **depois** de o texto persistir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CaptureToDbPipelineTest {

    @get:Rule
    val tmp = TemporaryFolder()

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
    fun `pcm com fala percorre captura ate virar segmento no banco`() = runBlocking {
        val store = FileEphemeralAudioStore(tmp.newFolder())
        val access = EphemeralAudioChunkAccess(store, sampleRate = 1000)
        val queue = TranscriptionQueue(
            audio = access,
            provider = FakeTranscriptionProvider(),
            segments = RoomSegmentSink(db.segmentDao()),
            gaps = RoomGapSink(db.timelineGapDao()),
        )

        var ids = 0
        val capture = CapturePipeline(
            chunker = PcmChunker(sampleRate = 1000, targetMs = 100), // 100 amostras/chunk
            vad = EnergyVad(rmsThreshold = 500.0),
            store = store,
            captureStartMs = 0,
            newChunkId = { "c${++ids}" },
            onStored = { sc ->
                access.register(sc.chunkId, sc.startedAtMs, sc.durationMs)
                queue.enqueue(PendingChunk(sc.chunkId, sc.startedAtMs, sc.durationMs, sc.sizeBytes.toLong()))
            },
        )

        capture.onAudio(ShortArray(100) { 3000 })  // fala → grava + enfileira
        capture.onAudio(ShortArray(100))           // silêncio → descartado
        val transcribed = queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = true, idle = false))

        assertEquals(1, transcribed)
        assertEquals(1, pipelineDroppedSilence(capture))
        val rows = db.segmentDao().observeInRange(0, Long.MAX_VALUE).first()
        assertEquals(1, rows.size)
        assertEquals("c1:0", rows.single().id)
        // Áudio destruído após o texto persistir (regra 2) — nada sobra no store.
        assertTrue(store.activeChunkIds().isEmpty())
    }

    private fun pipelineDroppedSilence(p: CapturePipeline) = p.droppedSilenceCount
}
