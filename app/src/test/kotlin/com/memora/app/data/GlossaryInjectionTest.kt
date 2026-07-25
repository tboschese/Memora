package com.memora.app.data

import com.memora.core.glossary.GlossaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryInjectionTest {

    private val entries = listOf(
        GlossaryEntry(id = "g1", canonical = "Kubernetes", variants = listOf("kubernetis")),
        GlossaryEntry(id = "g2", canonical = "Memora", variants = listOf("memra")),
    )

    @Test
    fun `whisper initial prompt lists the canonical spellings`() {
        val prompt = GlossaryInjection.whisperInitialPrompt(entries)
        assertTrue(prompt.contains("Kubernetes"))
        assertTrue(prompt.contains("Memora"))
    }

    @Test
    fun `whisper initial prompt respects a tiny budget`() {
        // orçamento minúsculo: cabe no máximo um termo, nunca os dois.
        val prompt = GlossaryInjection.whisperInitialPrompt(entries, tokenBudget = 3)
        assertTrue(!prompt.contains(", ")) // sem separador ⇒ 0 ou 1 termo
    }

    @Test
    fun `digest terms are the canonical spellings`() {
        assertEquals(listOf("Kubernetes", "Memora"), GlossaryInjection.digestTerms(entries))
    }

    @Test
    fun `an empty glossary yields an empty prompt and no terms`() {
        assertEquals("", GlossaryInjection.whisperInitialPrompt(emptyList()))
        assertTrue(GlossaryInjection.digestTerms(emptyList()).isEmpty())
    }
}
