package com.memora.app.data

import com.memora.core.transcription.TranscriptResult
import com.memora.core.transcription.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentMapperTest {

    @Test
    fun `mapeia cada segmento com id derivado de chunkId e indice`() {
        val result = TranscriptResult(
            chunkId = "c42",
            language = "pt",
            segments = listOf(
                TranscriptSegment(text = "olá", startMs = 0, endMs = 500, confidence = 0.9f),
                TranscriptSegment(text = "mundo", startMs = 500, endMs = 900, confidence = 0.8f),
            ),
        )

        val rows = result.toSegmentEntities(sessionId = "s1")

        assertEquals(listOf("c42:0", "c42:1"), rows.map { it.id })
        // Preserva a ordem dos segmentos do resultado.
        assertEquals(listOf("olá", "mundo"), rows.map { it.text })
        assertTrue(rows.all { it.chunkId == "c42" && it.language == "pt" && it.sessionId == "s1" })
        // Nunca chutar speaker: default UNKNOWN até a Fase 2.
        assertTrue(rows.all { it.speaker == "UNKNOWN" })
        assertEquals(500, rows[1].startMs)
        assertEquals(0.9f, rows[0].confidence, 0f)
    }

    @Test
    fun `resultado sem segmentos gera lista vazia`() {
        val result = TranscriptResult(chunkId = "empty", language = "pt", segments = emptyList())
        assertTrue(result.toSegmentEntities().isEmpty())
    }
}
