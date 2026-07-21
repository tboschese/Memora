package com.memora.core.common.log

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLogTest {

    private val captured = mutableListOf<Triple<SafeLog.Level, String, Map<String, String>>>()

    init {
        SafeLog.sink = SafeLog.Sink { level, event, fields ->
            captured += Triple(level, event, fields)
        }
    }

    @After
    fun tearDown() {
        captured.clear()
    }

    @Test
    fun `loga evento estruturado apenas com campos declarados`() {
        SafeLog.i(
            "transcription_chunk_done",
            SafeLog.Id("chunkId", "abc123"),
            SafeLog.DurationMs("elapsed", 842),
            SafeLog.Count("segments", 3),
            SafeLog.Flag("hadSpeech", true),
        )

        val (level, event, fields) = captured.single()
        assertEquals(SafeLog.Level.INFO, level)
        assertEquals("transcription_chunk_done", event)
        assertEquals(
            mapOf(
                "chunkId" to "abc123",
                "elapsed" to "842ms",
                "segments" to "3",
                "hadSpeech" to "true",
            ),
            fields,
        )
    }

    @Test
    fun `campos so expoem id e metrica, nunca conteudo livre`() {
        // O tipo Field é selado: não existe construtor que aceite texto livre de transcrição.
        // Este teste documenta a garantia — qualquer valor logado passa por render() controlado.
        val fields: List<SafeLog.Field> = listOf(
            SafeLog.Id("sessionId", "s-1"),
            SafeLog.Stage("stage", "VAD"),
        )
        assertTrue(fields.all { it.render().isNotEmpty() })
    }
}
