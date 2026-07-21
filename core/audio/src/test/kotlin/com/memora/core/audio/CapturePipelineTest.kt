package com.memora.core.audio

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePipelineTest {

    // 1000 Hz, 100ms → 100 amostras/chunk.
    private fun chunker() = PcmChunker(sampleRate = 1000, targetMs = 100)

    private fun loud(n: Int) = ShortArray(n) { 3000 }   // acima do limiar do EnergyVad
    private fun silence(n: Int) = ShortArray(n)          // zeros

    private class Collector {
        val stored = mutableListOf<CapturePipeline.StoredChunk>()
        val ids = generateSequence(1) { it + 1 }.iterator()
        fun nextId() = "c${ids.next()}"
    }

    @Test
    fun `chunk com fala e gravado e emitido, silencio e descartado`() = runTest {
        val store = InMemoryEphemeralAudioStore()
        val col = Collector()
        val pipeline = CapturePipeline(
            chunker = chunker(),
            vad = EnergyVad(rmsThreshold = 500.0),
            store = store,
            captureStartMs = 10_000,
            newChunkId = col::nextId,
            onStored = { col.stored += it },
        )

        pipeline.onAudio(loud(100))     // 1 chunk com fala → gravado
        pipeline.onAudio(silence(100))  // 1 chunk silencioso → descartado

        assertEquals(1, col.stored.size)
        assertEquals(1, pipeline.droppedSilenceCount)
        val chunk = col.stored.single()
        assertEquals("c1", chunk.chunkId)
        assertEquals(10_000, chunk.startedAtMs)
        assertEquals(100, chunk.durationMs)     // 100 amostras a 1000 Hz = 100ms
        assertEquals(200, chunk.sizeBytes)      // 100 amostras × 2 bytes
        assertTrue("c1" in store.activeChunkIds())
    }

    @Test
    fun `startedAtMs avanca considerando ate os chunks descartados`() = runTest {
        val store = InMemoryEphemeralAudioStore()
        val col = Collector()
        val pipeline = CapturePipeline(
            chunker = chunker(),
            vad = EnergyVad(rmsThreshold = 500.0),
            store = store,
            captureStartMs = 0,
            newChunkId = col::nextId,
            onStored = { col.stored += it },
        )

        pipeline.onAudio(silence(100)) // descartado, mas consome 100ms
        pipeline.onAudio(loud(100))    // gravado, começa em 100ms

        assertEquals(1, col.stored.size)
        assertEquals(100, col.stored.single().startedAtMs)
    }

    @Test
    fun `stop emite o chunk parcial restante`() = runTest {
        val store = InMemoryEphemeralAudioStore()
        val col = Collector()
        val pipeline = CapturePipeline(
            chunker = chunker(),
            vad = EnergyVad(rmsThreshold = 500.0),
            store = store,
            captureStartMs = 0,
            newChunkId = col::nextId,
            onStored = { col.stored += it },
        )

        pipeline.onAudio(loud(40)) // abaixo de 1 chunk (100): nada emitido ainda
        assertTrue(col.stored.isEmpty())

        pipeline.stop() // flush do parcial (40 amostras)
        assertEquals(1, col.stored.size)
        assertEquals(40, col.stored.single().durationMs)
    }
}
