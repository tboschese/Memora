package com.memora.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmChunkerTest {

    // 1000 Hz, 100ms → 100 amostras por chunk (matemática fácil).
    private fun chunker() = PcmChunker(sampleRate = 1000, targetMs = 100)

    @Test
    fun `emite um chunk ao completar o alvo`() {
        val c = chunker()
        val emitted = c.feed(ShortArray(100) { 1 })
        assertEquals(1, emitted.size)
        assertEquals(100, emitted[0].size)
        assertEquals(0, c.pendingMs())
    }

    @Test
    fun `acumula sem emitir quando abaixo do alvo`() {
        val c = chunker()
        val emitted = c.feed(ShortArray(40))
        assertTrue(emitted.isEmpty())
        assertEquals(40, c.pendingMs()) // 40 amostras a 1000 Hz = 40ms
    }

    @Test
    fun `feed grande emite multiplos chunks e guarda o resto`() {
        val c = chunker()
        val emitted = c.feed(ShortArray(250)) // 2 chunks completos + 50 de resto
        assertEquals(2, emitted.size)
        assertEquals(50, c.pendingMs())
    }

    @Test
    fun `chunks respeitam a fronteira acumulando entre feeds`() {
        val c = chunker()
        assertTrue(c.feed(ShortArray(60)).isEmpty())
        val emitted = c.feed(ShortArray(60)) // 60 + 60 = 120 → 1 chunk + 20 de resto
        assertEquals(1, emitted.size)
        assertEquals(20, c.pendingMs())
    }

    @Test
    fun `flush emite o resto e esvazia`() {
        val c = chunker()
        c.feed(ShortArray(30))
        val rest = c.flush()
        assertEquals(30, rest?.size)
        assertNull(c.flush())
        assertEquals(0, c.pendingMs())
    }

    @Test
    fun `preserva o conteudo das amostras na fronteira do chunk`() {
        val c = PcmChunker(sampleRate = 1000, targetMs = 10) // 10 amostras/chunk
        val input = ShortArray(10) { (it + 1).toShort() } // 1..10
        val emitted = c.feed(input)
        assertEquals(1, emitted.size)
        assertTrue(emitted[0].contentEquals(input))
    }
}
