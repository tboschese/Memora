package com.memora.app.data

import com.memora.core.glossary.GlossaryCorrector
import com.memora.core.glossary.GlossaryEntry
import com.memora.core.transcription.TranscriptResult
import com.memora.core.transcription.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptGlossaryCorrectionTest {

    private val corrector = GlossaryCorrector(
        listOf(GlossaryEntry(id = "g1", canonical = "Kubernetes", variants = listOf("kubernetis", "cubernetes"))),
    )

    private fun result(vararg texts: String) = TranscriptResult(
        chunkId = "c1",
        language = "pt",
        segments = texts.mapIndexed { i, t -> TranscriptSegment(text = t, startMs = i * 10L, endMs = i * 10L + 5, confidence = 0.9f) },
    )

    @Test
    fun `corrects every segment's text`() {
        val corrected = result("subi no kubernetis", "de novo no cubernetes").correctedBy(corrector)

        assertEquals("subi no Kubernetes", corrected.segments[0].text)
        assertEquals("de novo no Kubernetes", corrected.segments[1].text)
    }

    @Test
    fun `preserves timing and metadata`() {
        val original = result("kubernetis")
        val corrected = original.correctedBy(corrector)

        assertEquals(original.chunkId, corrected.chunkId)
        assertEquals(original.language, corrected.language)
        assertEquals(original.segments[0].startMs, corrected.segments[0].startMs)
        assertEquals(original.segments[0].confidence, corrected.segments[0].confidence, 0f)
    }

    @Test
    fun `text without variants is unchanged`() {
        val corrected = result("nada a corrigir aqui").correctedBy(corrector)
        assertEquals("nada a corrigir aqui", corrected.segments[0].text)
    }
}
