package com.memora.core.transcription

import com.memora.core.common.model.AudioChunk
import com.memora.core.transcription.fake.FakeTranscriptionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TranscriptionQueueTest {

    // --- Fakes de colaborador -------------------------------------------------

    /** Guarda o áudio por chunkId e registra o que foi destruído (na ordem). */
    private class FakeAudio : AudioChunkAccess {
        val stored = LinkedHashMap<String, AudioChunk>()
        val destroyed = mutableListOf<String>()

        fun put(chunkId: String, durationMs: Long) {
            stored[chunkId] = AudioChunk(
                id = chunkId,
                pcm16 = ShortArray(4),
                sampleRate = 16_000,
                startedAt = Instant.EPOCH,
                durationMs = durationMs,
            )
        }

        override suspend fun load(chunkId: String): AudioChunk? = stored[chunkId]

        override suspend fun destroy(chunkId: String) {
            stored.remove(chunkId)
            destroyed += chunkId
        }
    }

    private class RecordingSegments(private val events: MutableList<String>) : SegmentSink {
        val persisted = mutableListOf<String>()
        override suspend fun persist(result: TranscriptResult) {
            persisted += result.chunkId
            events += "persist:${result.chunkId}"
        }
    }

    private class RecordingGaps : GapSink {
        val gaps = mutableListOf<TranscriptionGap>()
        override suspend fun recordGap(gap: TranscriptionGap) {
            gaps += gap
        }
    }

    /** Provider que falha as primeiras [failTimes] chamadas de cada chunk e depois sucede. */
    private class FlakyProvider(private val failTimes: Int) : TranscriptionProvider {
        private val calls = HashMap<String, Int>()
        override suspend fun transcribe(chunk: AudioChunk, options: WhisperOptions): TranscriptResult {
            val n = (calls[chunk.id] ?: 0) + 1
            calls[chunk.id] = n
            if (n <= failTimes) error("boom")
            return TranscriptResult(chunk.id, "pt", emptyList())
        }
    }

    private fun chunk(id: String, bytes: Long, startedAtMs: Long = 0, durationMs: Long = 1_000) =
        PendingChunk(id, startedAtMs = startedAtMs, durationMs = durationMs, sizeBytes = bytes)

    // --- Testes ---------------------------------------------------------------

    @Test
    fun `contínuo transcreve na ordem, persiste texto e destrói o áudio`() = runTest {
        val audio = FakeAudio().apply { put("a", 1_000); put("b", 1_000) }
        val segments = RecordingSegments(mutableListOf())
        val queue = TranscriptionQueue(audio, FakeTranscriptionProvider(), segments, RecordingGaps())

        queue.enqueue(chunk("a", bytes = 10))
        queue.enqueue(chunk("b", bytes = 10))
        val done = queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = false, idle = false))

        assertEquals(2, done)
        assertEquals(listOf("a", "b"), segments.persisted)
        assertEquals(listOf("a", "b"), audio.destroyed)
        assertEquals(0, queue.pendingCount())
    }

    @Test
    fun `áudio só é destruído DEPOIS de o texto ser persistido`() = runTest {
        val events = mutableListOf<String>()
        val audio = FakeAudio().apply { put("a", 1_000) }
        // Envolve destroy para registrar no mesmo log de eventos que o persist.
        val trackingAudio = object : AudioChunkAccess {
            override suspend fun load(chunkId: String) = audio.load(chunkId)
            override suspend fun destroy(chunkId: String) {
                events += "destroy:$chunkId"
                audio.destroy(chunkId)
            }
        }
        val segments = RecordingSegments(events)
        val queue = TranscriptionQueue(trackingAudio, FakeTranscriptionProvider(), segments, RecordingGaps())

        queue.enqueue(chunk("a", bytes = 10))
        queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = true, idle = false))

        assertEquals(listOf("persist:a", "destroy:a"), events)
    }

    @Test
    fun `lote não drena com bateria e não-ocioso, drena ao carregar`() = runTest {
        val audio = FakeAudio().apply { put("a", 1_000) }
        val segments = RecordingSegments(mutableListOf())
        val queue = TranscriptionQueue(audio, FakeTranscriptionProvider(), segments, RecordingGaps())
        queue.enqueue(chunk("a", bytes = 10))

        assertEquals(0, queue.drain(DrainMode.BATCH, DeviceState(charging = false, idle = false)))
        assertEquals(1, queue.pendingCount())

        assertEquals(1, queue.drain(DrainMode.BATCH, DeviceState(charging = true, idle = false)))
        assertEquals(0, queue.pendingCount())
        assertEquals(listOf("a"), segments.persisted)
    }

    @Test
    fun `estouro de limite descarta os mais antigos com gap e destrói o áudio`() = runTest {
        val audio = FakeAudio().apply { put("a", 500); put("b", 500); put("c", 500) }
        val gaps = RecordingGaps()
        // Limite 120 bytes. "a" e "b" (50 cada) cabem; ao enfileirar "c" (100) o total vai a 200
        // e os dois mais antigos são despejados numa única passagem → um único gap.
        val queue = TranscriptionQueue(
            audio, FakeTranscriptionProvider(), RecordingSegments(mutableListOf()), gaps,
            maxQueueBytes = 120,
        )

        queue.enqueue(chunk("a", bytes = 50, startedAtMs = 0, durationMs = 500))
        queue.enqueue(chunk("b", bytes = 50, startedAtMs = 500, durationMs = 500))
        queue.enqueue(chunk("c", bytes = 100, startedAtMs = 1_000, durationMs = 500))

        // Só "c" sobra (100 <= 120); "a" e "b" foram descartados.
        assertEquals(100, queue.pendingBytes())
        assertEquals(listOf("a", "b"), audio.destroyed.sorted())
        assertEquals(1, gaps.gaps.size)
        val gap = gaps.gaps.single()
        assertEquals(GapReason.QUEUE_OVERFLOW, gap.reason)
        assertEquals(0, gap.fromMs)
        assertEquals(1_000, gap.toMs) // fim do "b" (500 + 500)
    }

    @Test
    fun `nunca descarta o único chunk mesmo acima do limite`() = runTest {
        val audio = FakeAudio().apply { put("big", 1_000) }
        val gaps = RecordingGaps()
        val queue = TranscriptionQueue(
            audio, FakeTranscriptionProvider(), RecordingSegments(mutableListOf()), gaps,
            maxQueueBytes = 50,
        )

        queue.enqueue(chunk("big", bytes = 999))

        assertEquals(1, queue.pendingCount())
        assertTrue(gaps.gaps.isEmpty())
    }

    @Test
    fun `áudio ausente registra gap AUDIO_MISSING e não persiste`() = runTest {
        val audio = FakeAudio() // nada armazenado: load devolve null
        val segments = RecordingSegments(mutableListOf())
        val gaps = RecordingGaps()
        val queue = TranscriptionQueue(audio, FakeTranscriptionProvider(), segments, gaps)

        queue.enqueue(chunk("ghost", bytes = 10, startedAtMs = 200, durationMs = 300))
        queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = true, idle = true))

        assertTrue(segments.persisted.isEmpty())
        assertEquals(0, queue.pendingCount())
        assertEquals(1, gaps.gaps.size)
        assertEquals(GapReason.AUDIO_MISSING, gaps.gaps.single().reason)
        assertEquals(200, gaps.gaps.single().fromMs)
        assertEquals(500, gaps.gaps.single().toMs)
    }

    @Test
    fun `falha transitória é retentada e depois transcreve`() = runTest {
        val audio = FakeAudio().apply { put("a", 1_000) }
        val segments = RecordingSegments(mutableListOf())
        val gaps = RecordingGaps()
        val queue = TranscriptionQueue(
            audio, FlakyProvider(failTimes = 2), segments, gaps, maxAttempts = 3,
        )

        queue.enqueue(chunk("a", bytes = 10))
        val done = queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = true, idle = false))

        assertEquals(1, done)
        assertEquals(listOf("a"), segments.persisted)
        assertEquals(listOf("a"), audio.destroyed)
        assertTrue(gaps.gaps.isEmpty())
    }

    @Test
    fun `falha persistente desiste após maxAttempts com gap e destrói o áudio`() = runTest {
        val audio = FakeAudio().apply { put("a", 1_000) }
        val segments = RecordingSegments(mutableListOf())
        val gaps = RecordingGaps()
        val queue = TranscriptionQueue(
            audio, FlakyProvider(failTimes = 99), segments, gaps, maxAttempts = 3,
        )

        queue.enqueue(chunk("a", bytes = 10, startedAtMs = 10, durationMs = 40))
        val done = queue.drain(DrainMode.CONTINUOUS, DeviceState(charging = true, idle = false))

        assertEquals(0, done)
        assertTrue(segments.persisted.isEmpty())
        assertEquals(listOf("a"), audio.destroyed)
        assertEquals(0, queue.pendingCount())
        assertEquals(GapReason.TRANSCRIBE_FAILED, gaps.gaps.single().reason)
        assertEquals(10, gaps.gaps.single().fromMs)
        assertEquals(50, gaps.gaps.single().toMs)
        assertFalse(done > 0)
    }
}
